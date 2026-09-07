import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like, Between } from 'typeorm';
import { Restaurant } from './entities/restaurant.entity';

@Injectable()
export class RestaurantsService {
  constructor(
    @InjectRepository(Restaurant)
    private readonly restaurantRepository: Repository<Restaurant>,
  ) {}

  /**
   * Barcha restoranlarni olish (filtrlash bilan)
   */
  async findAll(params: {
    search?: string;
    page?: number;
    limit?: number;
    latitude?: number;
    longitude?: number;
    maxDistanceKm?: number;
  }): Promise<{ data: Restaurant[]; total: number; page: number; limit: number }> {
    const search = params.search;
    const page = Math.max(1, Number(params.page) || 1);
    const limit = Math.max(1, Math.min(100, Number(params.limit) || 20));

    const queryBuilder = this.restaurantRepository
      .createQueryBuilder('restaurant')
      .where('restaurant.isActive = :isActive', { isActive: true });

    if (search) {
      queryBuilder.andWhere('LOWER(restaurant.name) LIKE LOWER(:search)', {
        search: `%${search}%`,
      });
    }

    // Agar koordinatalar berilgan bo'lsa, masofani hisoblash (PostGIS)
    if (params.latitude && params.longitude) {
      queryBuilder.addSelect(
        `(6371 * acos(cos(radians(:lat)) * cos(radians(restaurant.latitude)) * cos(radians(restaurant.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(restaurant.latitude))))`,
        'distance',
      );
      queryBuilder.setParameters({
        lat: params.latitude,
        lng: params.longitude,
      });

      if (params.maxDistanceKm) {
        queryBuilder.andWhere(
          `(6371 * acos(cos(radians(:lat)) * cos(radians(restaurant.latitude)) * cos(radians(restaurant.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(restaurant.latitude)))) <= :maxDist`,
          { maxDist: params.maxDistanceKm },
        );
      }

      queryBuilder.orderBy('distance', 'ASC');
    } else {
      queryBuilder.orderBy('restaurant.rating', 'DESC');
    }

    const total = await queryBuilder.getCount();

    const data = await queryBuilder
      .skip((page - 1) * limit)
      .take(limit)
      .getMany();

    return { data, total, page, limit };
  }

  /**
   * Restoran detallari
   */
  async findById(id: string): Promise<Restaurant> {
    const restaurant = await this.restaurantRepository.findOne({
      where: { id },
      relations: ['categories', 'categories.items'],
    });
    if (!restaurant) {
      throw new NotFoundException('Restoran topilmadi');
    }
    return restaurant;
  }

  /**
   * Restoran yaratish (owner uchun)
   */
  async create(ownerId: string, data: Partial<Restaurant>): Promise<Restaurant> {
    const restaurant = this.restaurantRepository.create({
      ...data,
      ownerId,
    });
    return this.restaurantRepository.save(restaurant);
  }

  /**
   * Restoran ma'lumotlarini yangilash
   */
  async update(
    id: string,
    ownerId: string,
    data: Partial<Restaurant>,
  ): Promise<Restaurant> {
    const restaurant = await this.restaurantRepository.findOne({
      where: { id, ownerId },
    });
    if (!restaurant) {
      throw new NotFoundException('Restoran topilmadi');
    }
    Object.assign(restaurant, data);
    return this.restaurantRepository.save(restaurant);
  }

  /**
   * Owner'ning restoranlarini olish
   */
  async findByOwner(ownerId: string): Promise<Restaurant[]> {
    return this.restaurantRepository.find({
      where: { ownerId },
      relations: ['categories'],
      order: { createdAt: 'DESC' },
    });
  }

  /**
   * Yetkazish narxini masofaga qarab hisoblash
   */
  calculateDeliveryFee(
    restaurant: Restaurant,
    customerLat: number,
    customerLng: number,
  ): { distanceKm: number; deliveryFee: number } {
    const distanceKm = this.calculateDistance(
      restaurant.latitude,
      restaurant.longitude,
      customerLat,
      customerLng,
    );

    const deliveryFee =
      Number(restaurant.deliveryFeeBase) +
      Number(restaurant.deliveryFeePerKm) * distanceKm;

    return {
      distanceKm: Math.round(distanceKm * 10) / 10,
      deliveryFee: Math.round(deliveryFee),
    };
  }

  /**
   * Haversine formulasi bilan ikki nuqta orasidagi masofani hisoblash (km)
   */
  private calculateDistance(
    lat1: number,
    lng1: number,
    lat2: number,
    lng2: number,
  ): number {
    const R = 6371; // Yer radiusi (km)
    const dLat = this.toRad(lat2 - lat1);
    const dLng = this.toRad(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) *
        Math.cos(this.toRad(lat2)) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private toRad(deg: number): number {
    return deg * (Math.PI / 180);
  }
}
