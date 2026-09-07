import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
} from '@nestjs/websockets';
import { Logger, Inject, forwardRef } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { RedisService } from '../redis/redis.service';
import { DispatcherService } from '../dispatcher/dispatcher.service';
import { Order } from '../orders/entities/order.entity';

interface AuthenticatedSocket extends Socket {
  userId?: string;
  userRole?: string;
  courierId?: string;
}

@WebSocketGateway({
  cors: {
    origin: '*',
  },
  namespace: '/tracking',
})
export class TrackingGateway
  implements OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(TrackingGateway.name);

  // Foydalanuvchi va socket bog'lanish xaritasi
  private userSockets: Map<string, string> = new Map(); // userId -> socketId
  private courierSockets: Map<string, string> = new Map(); // courierId -> socketId

  constructor(
    private readonly jwtService: JwtService,
    private readonly redisService: RedisService,
    @Inject(forwardRef(() => DispatcherService))
    private readonly dispatcherService: DispatcherService,
  ) {}

  /**
   * WebSocket ulanish - JWT token bilan autentifikatsiya
   */
  async handleConnection(client: AuthenticatedSocket) {
    try {
      const token =
        client.handshake.auth?.token ||
        client.handshake.headers?.authorization?.replace('Bearer ', '');

      if (!token) {
        this.logger.warn(`❌ Token yo'q, ulanish rad etildi: ${client.id}`);
        client.disconnect();
        return;
      }

      const payload = this.jwtService.verify(token);
      client.userId = payload.sub;
      client.userRole = payload.role;

      // Foydalanuvchini socket xaritasiga qo'shish
      this.userSockets.set(payload.sub, client.id);

      this.logger.log(
        `✅ WebSocket ulandi: ${payload.sub} (${payload.role}) - Socket: ${client.id}`,
      );
    } catch (error) {
      this.logger.warn(`❌ Noto'g'ri token: ${client.id}`);
      client.disconnect();
    }
  }

  /**
   * WebSocket uzilish
   */
  handleDisconnect(client: AuthenticatedSocket) {
    if (client.userId) {
      this.userSockets.delete(client.userId);
      this.logger.log(`🔌 WebSocket uzildi: ${client.userId}`);
    }
    if (client.courierId) {
      this.courierSockets.delete(client.courierId);
    }
  }

  // ==================== Kuryer Events ====================

  /**
   * Kuryer o'z courier profile ID sini ro'yxatdan o'tkazadi
   */
  @SubscribeMessage('courier:register')
  handleCourierRegister(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() data: { courierId: string },
  ) {
    client.courierId = data.courierId;
    this.courierSockets.set(data.courierId, client.id);
    this.logger.log(`🚴 Kuryer ro'yxatdan o'tdi: ${data.courierId}`);
    return { event: 'courier:registered', data: { success: true } };
  }

  /**
   * Kuryer GPS lokatsiyasini yuboradi (har 5 soniyada)
   */
  @SubscribeMessage('courier:location')
  async handleCourierLocation(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody()
    data: { latitude: number; longitude: number; bearing?: number; speed?: number },
  ) {
    if (!client.courierId) return;

    // Redis GEO'ga saqlash
    await this.redisService.setCourierLocation(
      client.courierId,
      data.longitude,
      data.latitude,
    );

    this.logger.debug(
      `📍 Kuryer ${client.courierId}: lat=${data.latitude}, lng=${data.longitude}`,
    );
  }

  /**
   * Kuryer buyurtma taklifiga javob beradi
   */
  @SubscribeMessage('courier:offer-response')
  async handleOfferResponse(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() data: { orderId: string; accepted: boolean },
  ) {
    if (!client.courierId) return;

    if (data.accepted) {
      await this.dispatcherService.handleCourierAccept(
        client.courierId,
        data.orderId,
      );
    } else {
      await this.dispatcherService.handleCourierReject(
        client.courierId,
        data.orderId,
      );
    }
  }

  // ==================== Mijoz Events ====================

  /**
   * Mijoz buyurtma kuzatishni boshlaydi
   */
  @SubscribeMessage('customer:track-order')
  handleTrackOrder(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() data: { orderId: string },
  ) {
    // Buyurtma xonasiga qo'shish
    client.join(`order:${data.orderId}`);
    this.logger.log(
      `👁️ Mijoz ${client.userId} buyurtma ${data.orderId} ni kuzatmoqda`,
    );
    return { event: 'tracking:started', data: { orderId: data.orderId } };
  }

  /**
   * Mijoz kuzatishni to'xtatadi
   */
  @SubscribeMessage('customer:stop-tracking')
  handleStopTracking(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() data: { orderId: string },
  ) {
    client.leave(`order:${data.orderId}`);
    this.logger.log(
      `⏹️ Mijoz ${client.userId} kuzatishni to'xtatdi: ${data.orderId}`,
    );
  }

  // ==================== Restoran Events ====================

  /**
   * Restoran yangi buyurtmalarni kuzatishni boshlaydi
   */
  @SubscribeMessage('restaurant:listen')
  handleRestaurantListen(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() data: { restaurantId: string },
  ) {
    client.join(`restaurant:${data.restaurantId}`);
    this.logger.log(
      `🏪 Restoran ${data.restaurantId} yangi buyurtmalarni kuzatmoqda`,
    );
    return { event: 'restaurant:listening', data: { success: true } };
  }

  // ==================== Server -> Client Emitter Methods ====================

  /**
   * Restoranga yangi buyurtma xabari yuborish
   */
  notifyNewOrder(restaurantId: string, order: Order): void {
    this.server.to(`restaurant:${restaurantId}`).emit('restaurant:new-order', {
      orderId: order.id,
      orderNumber: order.orderNumber,
      totalAmount: order.totalAmount,
      deliveryAddress: order.deliveryAddress,
      customerNote: order.customerNote,
      items: order.items,
      createdAt: order.createdAt,
    });

    this.logger.log(
      `🔔 Restoran ${restaurantId} ga yangi buyurtma xabari yuborildi`,
    );
  }

  /**
   * Buyurtma status o'zgarishini barcha ishtirokchilarga yuborish
   */
  notifyOrderStatusChange(order: Order): void {
    // Buyurtma xonasidagi barcha mijozlarga
    this.server.to(`order:${order.id}`).emit('order:status-changed', {
      orderId: order.id,
      orderNumber: order.orderNumber,
      status: order.status,
      updatedAt: new Date().toISOString(),
    });

    // Mijozga individual xabar
    if (order.customerId) {
      const customerSocketId = this.userSockets.get(order.customerId);
      if (customerSocketId) {
        this.server.to(customerSocketId).emit('order:status-changed', {
          orderId: order.id,
          orderNumber: order.orderNumber,
          status: order.status,
        });
      }
    }

    // Restoranga xabar
    if (order.restaurantId) {
      this.server
        .to(`restaurant:${order.restaurantId}`)
        .emit('order:status-changed', {
          orderId: order.id,
          orderNumber: order.orderNumber,
          status: order.status,
        });
    }

    this.logger.log(
      `📢 Buyurtma ${order.id} status o'zgardi: ${order.status}`,
    );
  }

  /**
   * Kuryerga buyurtma taklifi yuborish
   */
  sendOrderOffer(courierId: string, offerData: any): void {
    const socketId = this.courierSockets.get(courierId);
    if (socketId) {
      this.server.to(socketId).emit('courier:new-offer', offerData);
      this.logger.log(`📨 Kuryer ${courierId} ga taklif yuborildi`);
    } else {
      this.logger.warn(
        `⚠️ Kuryer ${courierId} WebSocket orqali ulanmagan`,
      );
    }
  }

  /**
   * Kuryer lokatsiyasini buyurtma kuzatuvchilarga yuborish
   * (Mijoz xaritada ko'radi)
   */
  broadcastCourierLocation(
    orderId: string,
    courierId: string,
    latitude: number,
    longitude: number,
    bearing?: number,
  ): void {
    this.server.to(`order:${orderId}`).emit('courier:location-update', {
      courierId,
      latitude,
      longitude,
      bearing: bearing || 0,
      timestamp: new Date().toISOString(),
    });
  }
}
