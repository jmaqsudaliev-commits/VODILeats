import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';

import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { RestaurantsModule } from './restaurants/restaurants.module';
import { MenuModule } from './menu/menu.module';
import { OrdersModule } from './orders/orders.module';
import { CourierModule } from './courier/courier.module';
import { DispatcherModule } from './dispatcher/dispatcher.module';
import { TrackingModule } from './tracking/tracking.module';
import { RedisModule } from './redis/redis.module';
import { AdminModule } from './admin/admin.module';

import { User } from './users/entities/user.entity';
import { Restaurant } from './restaurants/entities/restaurant.entity';
import { Category } from './menu/entities/category.entity';
import { MenuItem } from './menu/entities/menu-item.entity';
import { Order } from './orders/entities/order.entity';
import { OrderItem } from './orders/entities/order-item.entity';
import { CourierProfile } from './courier/entities/courier-profile.entity';
import { Address } from './users/entities/address.entity';

const entities = [
  User,
  Restaurant,
  Category,
  MenuItem,
  Order,
  OrderItem,
  CourierProfile,
  Address,
];

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),

    // Rate Limiting / DDoS Protection (1000 req/min limit per IP for high throughput)
    ThrottlerModule.forRoot([
      {
        name: 'default',
        ttl: 60000,
        limit: 1000,
      },
    ]),

    // Dual-Mode Database: PostgreSQL (Production 1M+ Scale with pooling) OR SQLite (Local Dev)
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (configService: ConfigService) => {
        const dbType = configService.get<string>('DATABASE_TYPE', 'sqlite');

        if (dbType === 'sqlite') {
          return {
            type: 'sqlite',
            database: configService.get<string>('SQLITE_DATABASE', 'vodil_eats.sqlite'),
            entities,
            synchronize: true,
            logging: false,
          };
        }

        return {
          type: 'postgres',
          host: configService.get<string>('DATABASE_HOST', 'localhost'),
          port: configService.get<number>('DATABASE_PORT', 5432),
          username: configService.get<string>('DATABASE_USER', 'vodil_admin'),
          password: configService.get<string>('DATABASE_PASSWORD', 'vodil_secret_2024'),
          database: configService.get<string>('DATABASE_NAME', 'vodil_eats'),
          entities,
          synchronize: configService.get<string>('NODE_ENV') === 'development',
          logging: false,
          // High-throughput Connection Pooling for 1,000,000+ users
          extra: {
            max: 100, // Maximum pool connections
            min: 10,
            idleTimeoutMillis: 30000,
            connectionTimeoutMillis: 5000,
          },
          ssl: configService.get<string>('NODE_ENV') === 'production'
            ? { rejectUnauthorized: false }
            : false,
        };
      },
    }),

    RedisModule,
    AuthModule,
    UsersModule,
    RestaurantsModule,
    MenuModule,
    OrdersModule,
    CourierModule,
    DispatcherModule,
    TrackingModule,
    AdminModule,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
