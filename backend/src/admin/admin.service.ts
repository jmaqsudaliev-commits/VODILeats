import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { Order, OrderStatus } from '../orders/entities/order.entity';
import { Restaurant } from '../restaurants/entities/restaurant.entity';
import { CourierProfile, CourierStatus } from '../courier/entities/courier-profile.entity';
import { User, UserRole } from '../users/entities/user.entity';
import { MenuItem } from '../menu/entities/menu-item.entity';
import { TrackingGateway } from '../tracking/tracking.gateway';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(Order)
    private readonly orderRepo: Repository<Order>,
    @InjectRepository(Restaurant)
    private readonly restaurantRepo: Repository<Restaurant>,
    @InjectRepository(CourierProfile)
    private readonly courierProfileRepo: Repository<CourierProfile>,
    @InjectRepository(User)
    private readonly userRepo: Repository<User>,
    @InjectRepository(MenuItem)
    private readonly menuItemRepo: Repository<MenuItem>,
    private readonly trackingGateway: TrackingGateway,
  ) {}

  // 20-xonali o'zgarmas maxfiy xavfsizlik paroli (Faqat ilova egasi uchun)
  public static readonly MASTER_PASSWORD = 'VODIL_EATS_2026_UZB!';

  /**
   * Admin parolini tekshirish
   */
  verifyAdminKey(key: string): boolean {
    return key === AdminService.MASTER_PASSWORD;
  }

  /**
   * Platformaning umumiy jonli statistikasi
   */
  async getStats() {
    const totalOrders = await this.orderRepo.count();

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

    const activeOrders = await this.orderRepo.count({
      where: { status: In(activeStatuses) },
    });

    const deliveredOrders = await this.orderRepo.count({
      where: { status: OrderStatus.DELIVERED },
    });

    const cancelledOrders = await this.orderRepo.count({
      where: { status: OrderStatus.CANCELLED },
    });

    // Jami tushum
    const deliveredList = await this.orderRepo.find({
      where: { status: OrderStatus.DELIVERED },
      select: ['totalAmount'],
    });
    const totalRevenue = deliveredList.reduce(
      (sum, o) => sum + (Number(o.totalAmount) || 0),
      0,
    );

    const totalRestaurants = await this.restaurantRepo.count();
    const activeRestaurants = await this.restaurantRepo.count({
      where: { isActive: true },
    });

    const totalCouriers = await this.courierProfileRepo.count();
    const onlineCouriers = await this.courierProfileRepo.count({
      where: { status: In([CourierStatus.ONLINE, CourierStatus.BUSY]) },
    });

    const totalCustomers = await this.userRepo.count({
      where: { role: UserRole.CUSTOMER },
    });

    return {
      totalRevenue,
      totalOrders,
      activeOrders,
      deliveredOrders,
      cancelledOrders,
      totalRestaurants,
      activeRestaurants,
      totalCouriers,
      onlineCouriers,
      totalCustomers,
      systemHealth: '100% OK',
      highLoadWorkers: 4,
      serverTime: new Date().toISOString(),
    };
  }

  /**
   * Barcha buyurtmalar ro'yxati (filtrlash imkoniyati bilan)
   */
  async getAllOrders(status?: string, limit: number = 100) {
    const safeLimit = Math.max(1, Number(limit) || 100);
    const query = this.orderRepo
      .createQueryBuilder('order')
      .leftJoinAndSelect('order.restaurant', 'restaurant')
      .leftJoinAndSelect('order.customer', 'customer')
      .leftJoinAndSelect('order.courier', 'courier')
      .leftJoinAndSelect('order.items', 'items')
      .orderBy('order.createdAt', 'DESC')
      .take(safeLimit);

    if (status && status !== 'ALL') {
      query.andWhere('order.status = :status', { status });
    }

    return query.getMany();
  }

  /**
   * Barcha restoranlar ro'yxati
   */
  async getAllRestaurants() {
    return this.restaurantRepo.find({
      relations: ['categories', 'categories.items', 'owner'],
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Restoran faolligini o'zgartirish (On/Off)
   */
  async toggleRestaurantStatus(id: string, field: 'isActive' | 'isVerified') {
    const restaurant = await this.restaurantRepo.findOne({ where: { id } });
    if (!restaurant) throw new NotFoundException('Restoran topilmadi');

    restaurant[field] = !restaurant[field];
    return this.restaurantRepo.save(restaurant);
  }

  /**
   * Yangi real restoran yaratish (Super Admin)
   */
  async createRestaurant(data: {
    name: string;
    description?: string;
    address: string;
    phone: string;
    latitude?: number;
    longitude?: number;
    openTime?: string;
    closeTime?: string;
    avgDeliveryTimeMinutes?: number;
    imageUrl?: string;
  }) {
    let owner = await this.userRepo.findOne({
      where: { role: UserRole.RESTAURANT_OWNER },
    });
    if (!owner) {
      owner = await this.userRepo.findOne({ where: {} });
    }

    const restaurant = this.restaurantRepo.create({
      name: data.name,
      description: data.description || 'Milliy va mazali taomlar',
      address: data.address,
      phone: data.phone,
      latitude: Number(data.latitude) || 40.1800,
      longitude: Number(data.longitude) || 71.7200,
      openTime: data.openTime || '09:00',
      closeTime: data.closeTime || '23:00',
      avgDeliveryTimeMinutes: Number(data.avgDeliveryTimeMinutes) || 30,
      imageUrl: data.imageUrl || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500',
      coverImageUrl: data.imageUrl || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800',
      ownerId: owner ? owner.id : undefined,
      isActive: true,
      isVerified: true,
    });

    return this.restaurantRepo.save(restaurant);
  }

  /**
   * Yangi real kuryer ro'yxatdan o'tkazish (Super Admin)
   */
  async createCourier(data: {
    firstName: string;
    lastName?: string;
    phone: string;
    vehicleType?: string;
    vehiclePlateNumber?: string;
  }) {
    let user = await this.userRepo.findOne({ where: { phone: data.phone } });
    if (!user) {
      user = await this.userRepo.save(
        this.userRepo.create({
          firstName: data.firstName,
          lastName: data.lastName || '',
          phone: data.phone,
          role: UserRole.COURIER,
          isPhoneVerified: true,
        }),
      );
    }

    let profile = await this.courierProfileRepo.findOne({ where: { userId: user.id } });
    if (!profile) {
      profile = this.courierProfileRepo.create({
        userId: user.id,
        status: CourierStatus.ONLINE,
        vehicleType: (data.vehicleType as any) || 'motorcycle',
        vehiclePlateNumber: data.vehiclePlateNumber || '',
        currentLatitude: 40.1800 + (Math.random() - 0.5) * 0.01,
        currentLongitude: 71.7200 + (Math.random() - 0.5) * 0.01,
        isVerified: true,
        isActive: true,
      });
      await this.courierProfileRepo.save(profile);
    }

    return this.courierProfileRepo.findOne({
      where: { id: profile.id },
      relations: ['user'],
    });
  }

  /**
   * Barcha kuryerlar ro'yxati va ularning jonli lokatsiyasi
   */
  async getAllCouriers() {
    return this.courierProfileRepo.find({
      relations: ['user'],
      order: { status: 'ASC', updatedAt: 'DESC' },
    });
  }

  /**
   * Kuryer holatini almashtirish
   */
  async updateCourierStatus(id: string, status: CourierStatus) {
    const courier = await this.courierProfileRepo.findOne({ where: { id } });
    if (!courier) throw new NotFoundException('Kuryer topilmadi');

    courier.status = status;
    return this.courierProfileRepo.save(courier);
  }

  /**
   * Barcha mijozlar
   */
  async getAllCustomers() {
    return this.userRepo.find({
      where: { role: UserRole.CUSTOMER },
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Buyurtma statusini super-admin tomonidan majburiy o'zgartirish
   */
  async forceUpdateOrderStatus(orderId: string, status: OrderStatus) {
    const order = await this.orderRepo.findOne({
      where: { id: orderId },
      relations: ['items', 'restaurant', 'customer', 'courier'],
    });
    if (!order) throw new NotFoundException('Buyurtma topilmadi');

    order.status = status;
    const now = new Date();
    if (status === OrderStatus.CONFIRMED) order.confirmedAt = now;
    if (status === OrderStatus.PREPARING) order.preparingAt = now;
    if (status === OrderStatus.READY_FOR_PICKUP) order.readyAt = now;
    if (status === OrderStatus.COURIER_PICKED_UP) order.pickedUpAt = now;
    if (status === OrderStatus.DELIVERED) order.deliveredAt = now;
    if (status === OrderStatus.CANCELLED) order.cancelledAt = now;

    const saved = await this.orderRepo.save(order);
    this.trackingGateway.notifyOrderStatusChange(saved);
    return saved;
  }

  /**
   * Sinov (Demo) buyurtma yaratish - Dashboardda jonli sinash uchun
   */
  async createDemoOrder() {
    const restaurant = await this.restaurantRepo.findOne({
      where: {},
      relations: ['categories', 'categories.items'],
    });
    if (!restaurant) throw new NotFoundException('Restoranlar mavjud emas');

    let customer = await this.userRepo.findOne({
      where: { role: UserRole.CUSTOMER },
    });
    if (!customer) {
      customer = await this.userRepo.save(
        this.userRepo.create({
          phone: '+998901112233',
          firstName: 'Alisher',
          lastName: 'Navoiy',
          role: UserRole.CUSTOMER,
          isPhoneVerified: true,
        }),
      );
    }

    const firstItem = restaurant.categories?.[0]?.items?.[0];
    const itemName = firstItem ? firstItem.name : 'Vodil Osh (Demo)';
    const itemPrice = firstItem ? Number(firstItem.price) : 35000;

    const orderNumber = `VE-${Date.now().toString(36).toUpperCase()}-${Math.floor(
      1000 + Math.random() * 9000,
    )}`;

    const demoOrder = this.orderRepo.create({
      orderNumber,
      status: OrderStatus.PENDING,
      customerId: customer.id,
      restaurantId: restaurant.id,
      deliveryAddress: 'Vodil markazi, Mustaqillik shoh ko\'chasi, 14-uy',
      deliveryLatitude: 40.182,
      deliveryLongitude: 71.725,
      subtotal: itemPrice * 2,
      deliveryFee: 10000,
      totalAmount: itemPrice * 2 + 10000,
      distanceKm: 2.5,
      customerNote: 'Iltimos, salat va non ham qo\'shib yuboring.',
    });

    const saved = await this.orderRepo.save(demoOrder);
    this.trackingGateway.notifyNewOrder(restaurant.id, saved);
    return saved;
  }
}
