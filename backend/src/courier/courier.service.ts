import {
  Injectable,
  NotFoundException,
  Inject,
  forwardRef,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CourierProfile, CourierStatus } from './entities/courier-profile.entity';
import { RedisService } from '../redis/redis.service';
import { TrackingGateway } from '../tracking/tracking.gateway';

@Injectable()
export class CourierService {
  constructor(
    @InjectRepository(CourierProfile)
    private readonly courierRepository: Repository<CourierProfile>,
    private readonly redisService: RedisService,
    @Inject(forwardRef(() => TrackingGateway))
    private readonly trackingGateway: TrackingGateway,
  ) {}

  /**
   * Kuryer profilini yaratish
   */
  async createProfile(userId: string, data: Partial<CourierProfile>): Promise<CourierProfile> {
    const existing = await this.courierRepository.findOne({
      where: { userId },
    });
    if (existing) {
      return existing;
    }

    const profile = this.courierRepository.create({
      ...data,
      userId,
    });
    return this.courierRepository.save(profile);
  }

  /**
   * Kuryer profilini olish
   */
  async getProfile(userId: string): Promise<CourierProfile> {
    const profile = await this.courierRepository.findOne({
      where: { userId },
      relations: ['user'],
    });
    if (!profile) {
      throw new NotFoundException('Kuryer profili topilmadi');
    }
    return profile;
  }

  /**
   * Kuryer profilini ID bo'yicha olish
   */
  async getProfileById(id: string): Promise<CourierProfile> {
    const profile = await this.courierRepository.findOne({
      where: { id },
      relations: ['user'],
    });
    if (!profile) {
      throw new NotFoundException('Kuryer profili topilmadi');
    }
    return profile;
  }

  /**
   * Kuryer statusini o'zgartirish (online/offline/busy)
   */
  async updateStatus(userId: string, status: CourierStatus): Promise<CourierProfile> {
    const profile = await this.getProfile(userId);
    profile.status = status;
    await this.courierRepository.save(profile);

    // Redis'da ham statusni yangilash
    const redisStatus = status === CourierStatus.ONLINE ? 'online' :
                        status === CourierStatus.BUSY ? 'busy' : 'offline';
    await this.redisService.setCourierStatus(profile.id, redisStatus);

    if (status === CourierStatus.OFFLINE) {
      await this.redisService.removeCourierLocation(profile.id);
    }

    return profile;
  }

  /**
   * Kuryer lokatsiyasini yangilash
   */
  async updateLocation(
    userId: string,
    latitude: number,
    longitude: number,
  ): Promise<void> {
    const profile = await this.getProfile(userId);

    // PostgreSQL'da yangilash
    await this.courierRepository.update(profile.id, {
      currentLatitude: latitude,
      currentLongitude: longitude,
      lastLocationUpdate: new Date(),
    });

    // Redis GEO'da yangilash
    await this.redisService.setCourierLocation(
      profile.id,
      longitude,
      latitude,
    );

    // Agar kuryerning faol buyurtmasi bo'lsa, WebSocket orqali lokatsiyani yuborish
    if (profile.currentOrderId) {
      this.trackingGateway.broadcastCourierLocation(
        profile.currentOrderId,
        profile.id,
        latitude,
        longitude,
      );
    }
  }

  /**
   * Buyurtmani qabul qilish
   */
  async acceptOrder(userId: string, orderId: string): Promise<CourierProfile> {
    const profile = await this.getProfile(userId);
    profile.status = CourierStatus.BUSY;
    profile.currentOrderId = orderId;
    await this.courierRepository.save(profile);
    await this.redisService.setCourierStatus(profile.id, 'busy');
    return profile;
  }

  /**
   * Buyurtmani rad etish
   */
  async rejectOrder(userId: string, orderId: string): Promise<void> {
    // Dispatcher keyingi kuryerga taklif yuboradi
    console.log(`Kuryer ${userId} buyurtma ${orderId} ni rad etdi`);
  }

  /**
   * Yetkazishni yakunlash
   */
  async completeDelivery(userId: string): Promise<CourierProfile> {
    const profile = await this.getProfile(userId);
    profile.status = CourierStatus.ONLINE;
    profile.currentOrderId = null as any;
    profile.totalDeliveries += 1;
    profile.todayDeliveries += 1;
    await this.courierRepository.save(profile);
    await this.redisService.setCourierStatus(profile.id, 'online');
    return profile;
  }

  /**
   * Barcha online kuryerlarni olish
   */
  async getOnlineCouriers(): Promise<CourierProfile[]> {
    return this.courierRepository.find({
      where: { status: CourierStatus.ONLINE, isActive: true, isVerified: true },
      relations: ['user'],
    });
  }
}
