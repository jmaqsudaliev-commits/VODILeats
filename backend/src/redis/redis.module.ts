import { Module, Global } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { RedisService } from './redis.service';

@Global()
@Module({
  providers: [
    {
      provide: 'REDIS_CLIENT',
      useFactory: async (configService: ConfigService) => {
        try {
          const Redis = (await import('ioredis')).default;
          const host = configService.get<string>('REDIS_HOST', 'localhost');
          const port = configService.get<number>('REDIS_PORT', 6379);

          const client = new Redis({
            host,
            port,
            retryStrategy: (times: number) => {
              if (times > 3) return null; // stop reconnect spam if Redis not installed locally
              return Math.min(times * 200, 1000);
            },
            maxRetriesPerRequest: 1,
            enableOfflineQueue: false,
            lazyConnect: true,
          });

          client.on('connect', () => {
            console.log('✅ Redis Cluster/Node connected successfully');
          });

          client.on('error', (err: Error) => {
            // Log once softly, RedisService has in-memory fallback
          });

          await client.connect().catch(() => {
            console.log('ℹ️ Redis daemon not detected locally — using in-memory high-speed cache fallback');
          });

          return client;
        } catch {
          return null;
        }
      },
      inject: [ConfigService],
    },
    RedisService,
  ],
  exports: ['REDIS_CLIENT', RedisService],
})
export class RedisModule {}
