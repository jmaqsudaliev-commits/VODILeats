import {
  Injectable,
  NotFoundException,
  BadRequestException,
  UnauthorizedException,
  Inject,
  forwardRef,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { CourierProfile, CourierStatus } from './entities/courier-profile.entity';
import { User, UserRole } from '../users/entities/user.entity';
import { RedisService } from '../redis/redis.service';
import { TrackingGateway } from '../tracking/tracking.gateway';

@Injectable()
export class CourierService {
  constructor(
    @InjectRepository(CourierProfile)
    private readonly courierRepository: Repository<CourierProfile>,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    private readonly redisService: RedisService,
    @Inject(forwardRef(() => TrackingGateway))
    private readonly trackingGateway: TrackingGateway,
  ) {}

  /**
   * Kuryer ro'yxatdan o'tishi (Yangi kuryer - dastlab tasdiqlanmagan holda bo'ladi)
   */
  async register(data: {
    firstName: string;
    lastName?: string;
    phone: string;
    password: string;
    vehicleType?: string;
    vehiclePlateNumber?: string;
  }) {
    const phone = data.phone.trim();
    let user = await this.userRepository.findOne({ where: { phone } });
    if (user) {
      const existingProfile = await this.courierRepository.findOne({ where: { userId: user.id } });
      if (existingProfile) {
        throw new BadRequestException('Ushbu telefon raqami allaqachon kuryer sifatida ro\'yxatdan o\'tgan');
      }
    }

    const passwordHash = await bcrypt.hash(data.password, 10);
    if (!user) {
      user = this.userRepository.create({
        firstName: data.firstName,
        lastName: data.lastName || '',
        phone,
        passwordHash,
        role: UserRole.COURIER,
        isPhoneVerified: true,
        isActive: true,
      });
      user = await this.userRepository.save(user);
    } else {
      user.role = UserRole.COURIER;
      user.passwordHash = passwordHash;
      user.firstName = data.firstName;
      if (data.lastName) user.lastName = data.lastName;
      await this.userRepository.save(user);
    }

    const profile = this.courierRepository.create({
      userId: user.id,
      status: CourierStatus.OFFLINE,
      vehicleType: (data.vehicleType as any) || 'motorcycle',
      vehiclePlateNumber: data.vehiclePlateNumber || '',
      isVerified: false, // Admin tasdiqlashi zarur!
      isActive: true,
    });
    const savedProfile = await this.courierRepository.save(profile);

    return {
      success: true,
      isVerified: false,
      message: 'Ro\'yxatdan o\'tish muvaffaqiyatli yakunlandi. Administrator arizangizni ko\'rib chiqmoqda.',
      user: {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone,
      },
      profile: {
        id: savedProfile.id,
        isVerified: false,
        status: savedProfile.status,
        vehicleType: savedProfile.vehicleType,
      },
    };
  }

  /**
   * Kuryer login (Telefon + Parol)
   */
  async login(data: { phone: string; password: string }) {
    const phone = data.phone.trim();
    const user = await this.userRepository.findOne({
      where: { phone },
      select: ['id', 'firstName', 'lastName', 'phone', 'role', 'passwordHash', 'isActive'],
    });

    if (!user || user.role !== UserRole.COURIER) {
      throw new UnauthorizedException('Kuryer hisobi topilmadi yoki telefon raqam xato');
    }

    if (!user.passwordHash) {
      throw new BadRequestException('Ushbu kuryerga hali parol o\'rnatilmagan');
    }

    const isMatch = await bcrypt.compare(data.password, user.passwordHash);
    if (!isMatch) {
      throw new UnauthorizedException('Telefon raqam yoki parol noto\'g\'ri');
    }

    let profile = await this.courierRepository.findOne({ where: { userId: user.id } });
    if (!profile) {
      profile = await this.createProfile(user.id, { isVerified: false });
    }

    return {
      success: true,
      accessToken: 'courier_jwt_token_' + user.id,
      isVerified: profile.isVerified,
      user: {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone,
      },
      profile: {
        id: profile.id,
        isVerified: profile.isVerified,
        status: profile.status,
        vehicleType: profile.vehicleType,
        vehiclePlateNumber: profile.vehiclePlateNumber,
        todayDeliveries: profile.todayDeliveries,
        rating: profile.rating,
      },
    };
  }

  /**
   * Kuryer tasdiqlanganlik holatini tekshirish
   */
  async checkStatus(phone: string) {
    const user = await this.userRepository.findOne({ where: { phone: phone.trim() } });
    if (!user) throw new NotFoundException('Foydalanuvchi topilmadi');

    const profile = await this.courierRepository.findOne({ where: { userId: user.id } });
    if (!profile) throw new NotFoundException('Kuryer profili topilmadi');

    return {
      success: true,
      isVerified: profile.isVerified,
      status: profile.status,
      message: profile.isVerified ? 'Profil tasdiqlangan ✅' : 'Kutilmoqda (Admin tasdig\'i zarur) ⏳',
    };
  }

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
