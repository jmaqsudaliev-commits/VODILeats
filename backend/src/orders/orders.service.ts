import {
  Injectable,
  NotFoundException,
  BadRequestException,
  Inject,
  forwardRef,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { v4 as uuidv4 } from 'uuid';
import { Order, OrderStatus, PaymentMethod } from './entities/order.entity';
import { OrderItem } from './entities/order-item.entity';
import { RestaurantsService } from '../restaurants/restaurants.service';
import { MenuService } from '../menu/menu.service';
import { TrackingGateway } from '../tracking/tracking.gateway';

export interface CreateOrderDto {
  restaurantId: string;
  deliveryAddress: string;
  deliveryLatitude: number;
  deliveryLongitude: number;
  paymentMethod: PaymentMethod;
  customerNote?: string;
  items: Array<{
    menuItemId: string;
    quantity: number;
    specialInstructions?: string;
  }>;
}

@Injectable()
export class OrdersService {
  constructor(
    @InjectRepository(Order)
    private readonly orderRepository: Repository<Order>,
    @InjectRepository(OrderItem)
    private readonly orderItemRepository: Repository<OrderItem>,
    private readonly restaurantsService: RestaurantsService,
    private readonly menuService: MenuService,
    @Inject(forwardRef(() => TrackingGateway))
    private readonly trackingGateway: TrackingGateway,
  ) {}

  /**
   * Buyurtma raqamini generatsiya qilish
   */
  private generateOrderNumber(): string {
    const timestamp = Date.now().toString(36).toUpperCase();
    const random = Math.random().toString(36).substring(2, 6).toUpperCase();
    return `VE-${timestamp}-${random}`;
  }

  /**
   * Yangi buyurtma yaratish
   */
  async createOrder(customerId: string, dto: CreateOrderDto): Promise<Order> {
    const restaurant = await this.restaurantsService.findById(dto.restaurantId);

    // Menyu elementlarini olish va narxlarni hisoblash
    let subtotal = 0;
    const orderItems: Partial<OrderItem>[] = [];

    for (const item of dto.items) {
      const menuItem = await this.menuService.getMenuItemById(item.menuItemId);

      if (menuItem.isInStopList || !menuItem.isAvailable) {
        throw new BadRequestException(
          `"${menuItem.name}" hozirda mavjud emas (stop-listda)`,
        );
      }

      const totalPrice = Number(menuItem.price) * item.quantity;
      subtotal += totalPrice;

      orderItems.push({
        menuItemId: item.menuItemId,
        name: menuItem.name,
        price: menuItem.price,
        quantity: item.quantity,
        totalPrice,
        specialInstructions: item.specialInstructions,
      });
    }

    // Minimal buyurtma summasini tekshirish
    if (subtotal < Number(restaurant.minimumOrderAmount)) {
      throw new BadRequestException(
        `Minimal buyurtma summasi: ${restaurant.minimumOrderAmount} so'm`,
      );
    }

    // Yetkazish narxini hisoblash
    const { distanceKm, deliveryFee } =
      this.restaurantsService.calculateDeliveryFee(
        restaurant,
        dto.deliveryLatitude,
        dto.deliveryLongitude,
      );

    const totalAmount = subtotal + deliveryFee;

    // Buyurtmani yaratish
    const order = this.orderRepository.create({
      orderNumber: this.generateOrderNumber(),
      status: OrderStatus.PENDING,
      customerId,
      restaurantId: dto.restaurantId,
      deliveryAddress: dto.deliveryAddress,
      deliveryLatitude: dto.deliveryLatitude,
      deliveryLongitude: dto.deliveryLongitude,
      paymentMethod: dto.paymentMethod,
      customerNote: dto.customerNote,
      subtotal,
      deliveryFee,
      totalAmount,
      distanceKm,
      estimatedDeliveryTime: new Date(
        Date.now() + restaurant.avgDeliveryTimeMinutes * 60000,
      ),
    });

    const savedOrder = await this.orderRepository.save(order);

    // Buyurtma elementlarini saqlash
    const items = orderItems.map((item) =>
      this.orderItemRepository.create({
        ...item,
        orderId: savedOrder.id,
      }),
    );
    await this.orderItemRepository.save(items);

    // Restoranga WebSocket orqali yangi buyurtma xabari
    this.trackingGateway.notifyNewOrder(dto.restaurantId, savedOrder);

    return this.findById(savedOrder.id);
  }

  /**
   * Buyurtmani ID bo'yicha olish
   */
  async findById(id: string): Promise<Order> {
    const order = await this.orderRepository.findOne({
      where: { id },
      relations: ['items', 'restaurant', 'customer', 'courier'],
    });
    if (!order) {
      throw new NotFoundException('Buyurtma topilmadi');
    }
    return order;
  }

  /**
   * Buyurtma statusini o'zgartirish
   */
  async updateStatus(
    orderId: string,
    newStatus: OrderStatus,
    userId?: string,
  ): Promise<Order> {
    const order = await this.findById(orderId);

    // Status o'tishlarini tekshirish
    this.validateStatusTransition(order.status, newStatus);

    order.status = newStatus;

    // Vaqt belgilarni qo'yish
    const now = new Date();
    switch (newStatus) {
      case OrderStatus.CONFIRMED:
        order.confirmedAt = now;
        break;
      case OrderStatus.PREPARING:
        order.preparingAt = now;
        break;
      case OrderStatus.READY_FOR_PICKUP:
        order.readyAt = now;
        break;
      case OrderStatus.COURIER_PICKED_UP:
        order.pickedUpAt = now;
        break;
      case OrderStatus.DELIVERED:
        order.deliveredAt = now;
        break;
      case OrderStatus.CANCELLED:
        order.cancelledAt = now;
        break;
    }

    const updatedOrder = await this.orderRepository.save(order);

    // WebSocket orqali status o'zgarishini yuborish
    this.trackingGateway.notifyOrderStatusChange(updatedOrder);

    return updatedOrder;
  }

  /**
   * Status o'tishlarini validatsiya qilish
   */
  private validateStatusTransition(
    currentStatus: OrderStatus,
    newStatus: OrderStatus,
  ): void {
    const allowedTransitions: Record<OrderStatus, OrderStatus[]> = {
      [OrderStatus.PENDING]: [OrderStatus.CONFIRMED, OrderStatus.CANCELLED],
      [OrderStatus.CONFIRMED]: [OrderStatus.PREPARING, OrderStatus.CANCELLED],
      [OrderStatus.PREPARING]: [
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.CANCELLED,
      ],
      [OrderStatus.READY_FOR_PICKUP]: [
        OrderStatus.COURIER_ASSIGNED,
        OrderStatus.CANCELLED,
      ],
      [OrderStatus.COURIER_ASSIGNED]: [
        OrderStatus.COURIER_PICKING_UP,
        OrderStatus.CANCELLED,
      ],
      [OrderStatus.COURIER_PICKING_UP]: [
        OrderStatus.COURIER_PICKED_UP,
        OrderStatus.CANCELLED,
      ],
      [OrderStatus.COURIER_PICKED_UP]: [
        OrderStatus.DELIVERING,
        OrderStatus.CANCELLED,
      ],
      [OrderStatus.DELIVERING]: [OrderStatus.DELIVERED, OrderStatus.CANCELLED],
      [OrderStatus.DELIVERED]: [],
      [OrderStatus.CANCELLED]: [],
    };

    if (!allowedTransitions[currentStatus]?.includes(newStatus)) {
      throw new BadRequestException(
        `"${currentStatus}" statusdan "${newStatus}" statusga o'tish mumkin emas`,
      );
    }
  }

  /**
   * Kuryerni buyurtmaga biriktirish
   */
  async assignCourier(orderId: string, courierId: string): Promise<Order> {
    const order = await this.findById(orderId);
    order.courierId = courierId;
    order.status = OrderStatus.COURIER_ASSIGNED;
    const updatedOrder = await this.orderRepository.save(order);
    this.trackingGateway.notifyOrderStatusChange(updatedOrder);
    return updatedOrder;
  }

  /**
   * Mijoz buyurtmalarini olish
   */
  async getCustomerOrders(
    customerId: string,
    page: number = 1,
    limit: number = 20,
  ): Promise<{ data: Order[]; total: number }> {
    const [data, total] = await this.orderRepository.findAndCount({
      where: { customerId },
      relations: ['items', 'restaurant'],
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { data, total };
  }

  /**
   * Restoran buyurtmalarini olish
   */
  async getRestaurantOrders(
    restaurantId: string,
    status?: OrderStatus,
    page: number = 1,
    limit: number = 20,
  ): Promise<{ data: Order[]; total: number }> {
    const where: any = { restaurantId };
    if (status) {
      where.status = status;
    }

    const [data, total] = await this.orderRepository.findAndCount({
      where,
      relations: ['items', 'customer'],
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return { data, total };
  }

  /**
   * Kuryer buyurtmalarini olish
   */
  async getCourierOrders(
    courierId: string,
    status?: OrderStatus,
  ): Promise<Order[]> {
    const where: any = { courierId };
    if (status) {
      where.status = status;
    }

    return this.orderRepository.find({
      where,
      relations: ['items', 'restaurant', 'customer'],
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Buyurtmani bekor qilish
   */
  async cancelOrder(orderId: string, reason: string): Promise<Order> {
    const order = await this.findById(orderId);
    order.status = OrderStatus.CANCELLED;
    order.cancelledAt = new Date();
    order.cancellationReason = reason;
    const updatedOrder = await this.orderRepository.save(order);
    this.trackingGateway.notifyOrderStatusChange(updatedOrder);
    return updatedOrder;
  }

  /**
   * Faol buyurtmalarni olish (restoran yoki mijoz uchun)
   */
  async getActiveOrders(restaurantId?: string): Promise<Order[]> {
    const activeStatuses = [
      OrderStatus.PENDING,
      OrderStatus.CONFIRMED,
      OrderStatus.PREPARING,
      OrderStatus.READY_FOR_PICKUP,
      OrderStatus.COURIER_ASSIGNED,
      OrderStatus.COURIER_PICKING_UP,
      OrderStatus.COURIER_PICKED_UP,
      OrderStatus.DELIVERING,
    ];

    const queryBuilder = this.orderRepository
      .createQueryBuilder('order')
      .where('order.status IN (:...statuses)', { statuses: activeStatuses })
      .leftJoinAndSelect('order.items', 'items')
      .leftJoinAndSelect('order.restaurant', 'restaurant')
      .leftJoinAndSelect('order.customer', 'customer')
      .orderBy('order.createdAt', 'DESC');

    if (restaurantId) {
      queryBuilder.andWhere('order.restaurantId = :restaurantId', {
        restaurantId,
      });
    }

    return queryBuilder.getMany();
  }
}
