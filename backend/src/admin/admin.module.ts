import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AdminController } from './admin.controller';
import { AdminService } from './admin.service';
import { Order } from '../orders/entities/order.entity';
import { Restaurant } from '../restaurants/entities/restaurant.entity';
import { CourierProfile } from '../courier/entities/courier-profile.entity';
import { User } from '../users/entities/user.entity';
import { MenuItem } from '../menu/entities/menu-item.entity';
import { TrackingModule } from '../tracking/tracking.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Order,
      Restaurant,
      CourierProfile,
      User,
      MenuItem,
    ]),
    TrackingModule,
  ],
  controllers: [AdminController],
  providers: [AdminService],
  exports: [AdminService],
})
export class AdminModule {}
