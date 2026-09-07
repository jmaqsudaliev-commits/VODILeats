import { Injectable, Inject, Optional } from '@nestjs/common';
import type Redis from 'ioredis';

interface MemoryCourierLocation {
  courierId: string;
  longitude: number;
  latitude: number;
  updatedAt: Date;
}

@Injectable()
export class RedisService {
  // In-memory fallback stores for zero-downtime local testing
  private memoryCache: Map<string, { value: string; expiresAt: number }> = new Map();
  private memoryLocations: Map<string, MemoryCourierLocation> = new Map();

  constructor(@Optional() @Inject('REDIS_CLIENT') private readonly redis: Redis | null) {}

  private isRedisConnected(): boolean {
    return this.redis !== null && this.redis.status === 'ready';
  }

  // ==================== GEO Operations ====================

  /**
   * Kuryer lokatsiyasini Redis GEO'ga yoki In-Memory store'ga saqlash
   */
  async setCourierLocation(
    courierId: string,
    longitude: number,
    latitude: number,
  ): Promise<void> {
    if (this.isRedisConnected()) {
      try {
        await this.redis!.geoadd('couriers:locations', longitude, latitude, courierId);
        await this.redis!.set(
          `courier:location:${courierId}`,
          JSON.stringify({ longitude, latitude, updatedAt: new Date().toISOString() }),
          'EX',
          300,
        );
        return;
      } catch (err) {
        // fall through to memory
      }
    }

    this.memoryLocations.set(courierId, {
      courierId,
      longitude,
      latitude,
      updatedAt: new Date(),
    });
    await this.set(`courier:location:${courierId}`, JSON.stringify({ longitude, latitude, updatedAt: new Date().toISOString() }), 300);
  }

  async removeCourierLocation(courierId: string): Promise<void> {
    if (this.isRedisConnected()) {
      try {
        await this.redis!.zrem('couriers:locations', courierId);
        await this.redis!.del(`courier:location:${courierId}`);
        return;
      } catch (err) {}
    }
    this.memoryLocations.delete(courierId);
    this.memoryCache.delete(`courier:location:${courierId}`);
  }

  async findNearbyCouriers(
    longitude: number,
    latitude: number,
    radiusKm: number,
    count: number = 10,
  ): Promise<Array<{ courierId: string; distance: number }>> {
    if (this.isRedisConnected()) {
      try {
        const results = await this.redis!.georadius(
          'couriers:locations',
          longitude,
          latitude,
          radiusKm,
          'km',
          'WITHDIST',
          'ASC',
          'COUNT',
          count,
        );

        return (results as any[]).map((result: any) => ({
          courierId: result[0] as string,
          distance: parseFloat(result[1] as string),
        }));
      } catch (err) {}
    }

    // In-memory Haversine distance formula
    const nearby: Array<{ courierId: string; distance: number }> = [];
    for (const [courierId, loc] of this.memoryLocations.entries()) {
      const dist = this.calculateHaversineDistance(latitude, longitude, loc.latitude, loc.longitude);
      if (dist <= radiusKm) {
        nearby.push({ courierId, distance: Math.round(dist * 100) / 100 });
      }
    }

    nearby.sort((a, b) => a.distance - b.distance);
    return nearby.slice(0, count);
  }

  async getCourierLocation(
    courierId: string,
  ): Promise<{ longitude: number; latitude: number; updatedAt: string } | null> {
    const raw = await this.get(`courier:location:${courierId}`);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  // ==================== Courier Status ====================

  async setCourierStatus(courierId: string, status: string): Promise<void> {
    await this.set(`courier:status:${courierId}`, status, 86400); // 24 hours
  }

  async getCourierStatus(courierId: string): Promise<string | null> {
    return await this.get(`courier:status:${courierId}`);
  }

  // ==================== OTP Management ====================

  async setOtp(phone: string, otp: string, ttlSeconds: number = 300): Promise<void> {
    await this.set(`otp:${phone}`, otp, ttlSeconds);
  }

  async getOtp(phone: string): Promise<string | null> {
    return await this.get(`otp:${phone}`);
  }

  async deleteOtp(phone: string): Promise<void> {
    await this.del(`otp:${phone}`);
  }

  // ==================== Order Dispatch State ====================

  async setOrderDispatchState(orderId: string, state: any, ttlSeconds: number = 1800): Promise<void> {
    await this.set(`dispatch:order:${orderId}`, JSON.stringify(state), ttlSeconds);
  }

  async getOrderDispatchState(orderId: string): Promise<any | null> {
    const raw = await this.get(`dispatch:order:${orderId}`);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  async removeOrderDispatchState(orderId: string): Promise<void> {
    await this.del(`dispatch:order:${orderId}`);
  }

  // ==================== General Cache Operations ====================

  async get(key: string): Promise<string | null> {
    if (this.isRedisConnected()) {
      try {
        return await this.redis!.get(key);
      } catch {}
    }

    const item = this.memoryCache.get(key);
    if (!item) return null;
    if (item.expiresAt < Date.now()) {
      this.memoryCache.delete(key);
      return null;
    }
    return item.value;
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    if (this.isRedisConnected()) {
      try {
        if (ttlSeconds) {
          await this.redis!.set(key, value, 'EX', ttlSeconds);
        } else {
          await this.redis!.set(key, value);
        }
        return;
      } catch {}
    }

    const expiresAt = ttlSeconds ? Date.now() + ttlSeconds * 1000 : Infinity;
    this.memoryCache.set(key, { value, expiresAt });
  }

  async del(key: string): Promise<void> {
    if (this.isRedisConnected()) {
      try {
        await this.redis!.del(key);
        return;
      } catch {}
    }
    this.memoryCache.delete(key);
  }

  // ==================== Rate Limiting ====================

  async isRateLimited(key: string, limit: number, windowSeconds: number): Promise<boolean> {
    const countKey = `ratelimit:${key}`;
    if (this.isRedisConnected()) {
      try {
        const current = await this.redis!.incr(countKey);
        if (current === 1) {
          await this.redis!.expire(countKey, windowSeconds);
        }
        return current > limit;
      } catch {}
    }

    const item = this.memoryCache.get(countKey);
    const now = Date.now();
    let current = 1;
    if (item && item.expiresAt > now) {
      current = parseInt(item.value, 10) + 1;
    }
    this.memoryCache.set(countKey, {
      value: current.toString(),
      expiresAt: now + windowSeconds * 1000,
    });
    return current > limit;
  }

  // ==================== Haversine Helper ====================

  private calculateHaversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Earth radius in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) *
        Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
