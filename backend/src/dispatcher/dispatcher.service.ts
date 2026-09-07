import {
  Injectable,
  Inject,
  forwardRef,
  Logger,
  OnModuleInit,
} from '@nestjs/common';
import { RedisService } from '../redis/redis.service';
import { OrdersService } from '../orders/orders.service';
import { CourierService } from '../courier/courier.service';
import { TrackingGateway } from '../tracking/tracking.gateway';
import { OrderStatus } from '../orders/entities/order.entity';

/**
 * DISPATCHER CORE ENGINE
 * 
 * Buyurtma tayyor bo'lganda (READY_FOR_PICKUP):
 * 1. Redis GEO yordamida 3 km radiusdagi bo'sh kuryerlarni topish
 * 2. Eng yaqin kuryerga taklif yuborish (WebSocket orqali)
 * 3. 30 soniya kutish - agar javob bo'lmasa, keyingi kuryerga
 * 4. Barcha kuryerlar rad etsa yoki hech kim topilmasa - restart
 */
@Injectable()
export class DispatcherService implements OnModuleInit {
  private readonly logger = new Logger(DispatcherService.name);
  private readonly SEARCH_RADIUS_KM = 3;
  private readonly OFFER_TIMEOUT_MS = 30000; // 30 soniya
  private readonly MAX_SEARCH_RADIUS_KM = 10;
  private readonly RADIUS_INCREMENT_KM = 2;

  // Faol dispatch jarayonlarni kuzatish
  private activeDispatches: Map<string, NodeJS.Timeout> = new Map();

  constructor(
    private readonly redisService: RedisService,
    @Inject(forwardRef(() => OrdersService))
    private readonly ordersService: OrdersService,
    @Inject(forwardRef(() => CourierService))
    private readonly courierService: CourierService,
    @Inject(forwardRef(() => TrackingGateway))
    private readonly trackingGateway: TrackingGateway,
  ) {}

  onModuleInit() {
    this.logger.log('🚀 Dispatcher Core Engine initialized');
  }

  /**
   * Buyurtma uchun kuryer qidirish jarayonini boshlash
   * Restoran buyurtmani "READY_FOR_PICKUP" holatiga o'tkazganda chaqiriladi
   */
  async dispatchOrder(orderId: string): Promise<void> {
    this.logger.log(`📦 Dispatch boshlandi: Order ${orderId}`);

    const order = await this.ordersService.findById(orderId);

    if (order.status !== OrderStatus.READY_FOR_PICKUP) {
      this.logger.warn(
        `⚠️ Buyurtma ${orderId} READY_FOR_PICKUP holatida emas. Joriy holat: ${order.status}`,
      );
      return;
    }

    // Restoran koordinatalarini olish (olib borish nuqtasi)
    const restaurantLat = order.restaurant.latitude;
    const restaurantLng = order.restaurant.longitude;

    // Dispatch holatini Redis'ga saqlash
    await this.redisService.setOrderDispatchState(orderId, {
      attemptedCouriers: [],
      expiresAt: new Date(Date.now() + 600000).toISOString(), // 10 daqiqa
    });

    // Eng yaqin kuryerlarni qidirish va taklif yuborish
    await this.findAndOfferToCourier(
      orderId,
      restaurantLat,
      restaurantLng,
      this.SEARCH_RADIUS_KM,
    );
  }

  /**
   * Redis GEO yordamida yaqin kuryerlarni topish va taklif yuborish
   */
  private async findAndOfferToCourier(
    orderId: string,
    latitude: number,
    longitude: number,
    radiusKm: number,
  ): Promise<void> {
    // Dispatch holatini olish
    const dispatchState = await this.redisService.getOrderDispatchState(orderId);
    if (!dispatchState) {
      this.logger.warn(`⚠️ Dispatch holati topilmadi: ${orderId}`);
      return;
    }

    const attemptedCouriers: string[] = dispatchState.attemptedCouriers || [];

    // Redis GEO'dan yaqin kuryerlarni topish
    const nearbyCouriers = await this.redisService.findNearbyCouriers(
      longitude,
      latitude,
      radiusKm,
      20, // Maksimal 20 ta natija
    );

    this.logger.log(
      `🔍 ${radiusKm} km radiusda ${nearbyCouriers.length} ta kuryer topildi`,
    );

    // Bo'sh (online) kuryerlarni filtrlash
    const availableCouriers = [];
    for (const courier of nearbyCouriers) {
      // Allaqachon taklif yuborilgan kuryerlarni o'tkazib yuborish
      if (attemptedCouriers.includes(courier.courierId)) {
        continue;
      }

      // Kuryer statusini tekshirish
      const status = await this.redisService.getCourierStatus(courier.courierId);
      if (status === 'online') {
        availableCouriers.push(courier);
      }
    }

    this.logger.log(
      `✅ ${availableCouriers.length} ta bo'sh kuryer mavjud`,
    );

    if (availableCouriers.length === 0) {
      // Radius kengaytirish
      if (radiusKm < this.MAX_SEARCH_RADIUS_KM) {
        const newRadius = radiusKm + this.RADIUS_INCREMENT_KM;
        this.logger.log(
          `📡 Radius kengaytirilmoqda: ${radiusKm}km -> ${newRadius}km`,
        );

        // 10 soniya kutib qayta qidirish
        const timeout = setTimeout(async () => {
          await this.findAndOfferToCourier(
            orderId,
            latitude,
            longitude,
            newRadius,
          );
        }, 10000);

        this.activeDispatches.set(orderId, timeout);
        return;
      }

      this.logger.warn(
        `❌ Hech qanday bo'sh kuryer topilmadi. Buyurtma: ${orderId}`,
      );

      // Radiusni reset qilib qayta qidirish (1 daqiqadan keyin)
      const timeout = setTimeout(async () => {
        // Dispatch holatini yangilash
        await this.redisService.setOrderDispatchState(orderId, {
          attemptedCouriers: [],
          expiresAt: new Date(Date.now() + 600000).toISOString(),
        });
        await this.findAndOfferToCourier(
          orderId,
          latitude,
          longitude,
          this.SEARCH_RADIUS_KM,
        );
      }, 60000);

      this.activeDispatches.set(orderId, timeout);
      return;
    }

    // Eng yaqin kuryerga taklif yuborish
    const selectedCourier = availableCouriers[0];
    await this.sendOfferToCourier(
      orderId,
      selectedCourier.courierId,
      selectedCourier.distance,
      latitude,
      longitude,
      attemptedCouriers,
    );
  }

  /**
   * Kuryerga buyurtma taklifi yuborish (WebSocket orqali)
   */
  private async sendOfferToCourier(
    orderId: string,
    courierId: string,
    distanceKm: number,
    restaurantLat: number,
    restaurantLng: number,
    previouslyAttempted: string[],
  ): Promise<void> {
    this.logger.log(
      `📨 Taklif yuborilmoqda: Kuryer ${courierId}, Masofa: ${distanceKm.toFixed(2)} km`,
    );

    const order = await this.ordersService.findById(orderId);

    // Dispatch holatini yangilash
    const attemptedCouriers = [...previouslyAttempted, courierId];
    await this.redisService.setOrderDispatchState(orderId, {
      currentCourierId: courierId,
      attemptedCouriers,
      expiresAt: new Date(Date.now() + this.OFFER_TIMEOUT_MS).toISOString(),
    });

    // WebSocket orqali kuryerga taklif yuborish
    this.trackingGateway.sendOrderOffer(courierId, {
      orderId: order.id,
      orderNumber: order.orderNumber,
      restaurantName: order.restaurant.name,
      restaurantAddress: order.restaurant.address,
      restaurantLatitude: restaurantLat,
      restaurantLongitude: restaurantLng,
      deliveryAddress: order.deliveryAddress,
      deliveryLatitude: order.deliveryLatitude,
      deliveryLongitude: order.deliveryLongitude,
      totalAmount: order.totalAmount,
      distanceToRestaurant: distanceKm,
      estimatedDeliveryDistance: order.distanceKm,
      timeoutSeconds: this.OFFER_TIMEOUT_MS / 1000,
    });

    // Timeout: agar 30 soniya ichida javob bo'lmasa, keyingi kuryerga o'tish
    const timeout = setTimeout(async () => {
      this.logger.log(
        `⏰ Timeout: Kuryer ${courierId} javob bermadi. Keyingi kuryerga o'tilmoqda...`,
      );

      await this.findAndOfferToCourier(
        orderId,
        restaurantLat,
        restaurantLng,
        this.SEARCH_RADIUS_KM,
      );
    }, this.OFFER_TIMEOUT_MS);

    this.activeDispatches.set(orderId, timeout);
  }

  /**
   * Kuryer buyurtmani qabul qilganda
   */
  async handleCourierAccept(
    courierId: string,
    orderId: string,
  ): Promise<void> {
    this.logger.log(
      `✅ Kuryer ${courierId} buyurtma ${orderId} ni qabul qildi`,
    );

    // Timeout'ni tozalash
    const timeout = this.activeDispatches.get(orderId);
    if (timeout) {
      clearTimeout(timeout);
      this.activeDispatches.delete(orderId);
    }

    // Dispatch holatini tozalash
    await this.redisService.removeOrderDispatchState(orderId);

    // Buyurtmaga kuryerni biriktirish
    await this.ordersService.assignCourier(orderId, courierId);

    // Kuryer statusini "busy" ga o'tkazish
    const courierProfile = await this.courierService.getProfileById(courierId);
    await this.courierService.acceptOrder(courierProfile.userId, orderId);

    this.logger.log(
      `🎉 Buyurtma ${orderId} -> Kuryer ${courierId} ga biriktirildi`,
    );
  }

  /**
   * Kuryer buyurtmani rad etganda
   */
  async handleCourierReject(
    courierId: string,
    orderId: string,
  ): Promise<void> {
    this.logger.log(
      `❌ Kuryer ${courierId} buyurtma ${orderId} ni rad etdi`,
    );

    // Timeout'ni tozalash
    const timeout = this.activeDispatches.get(orderId);
    if (timeout) {
      clearTimeout(timeout);
      this.activeDispatches.delete(orderId);
    }

    const order = await this.ordersService.findById(orderId);

    // Keyingi kuryerga taklif yuborish
    await this.findAndOfferToCourier(
      orderId,
      order.restaurant.latitude,
      order.restaurant.longitude,
      this.SEARCH_RADIUS_KM,
    );
  }

  /**
   * Dispatch jarayonini to'xtatish
   */
  cancelDispatch(orderId: string): void {
    const timeout = this.activeDispatches.get(orderId);
    if (timeout) {
      clearTimeout(timeout);
      this.activeDispatches.delete(orderId);
    }
    this.redisService.removeOrderDispatchState(orderId);
    this.logger.log(`🛑 Dispatch to'xtatildi: ${orderId}`);
  }
}
