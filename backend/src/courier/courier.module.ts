import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CourierService } from './courier.service';
import { CourierController } from './courier.controller';
import { CourierProfile } from './entities/courier-profile.entity';
import { User } from '../users/entities/user.entity';
import { TrackingModule } from '../tracking/tracking.module';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([CourierProfile, User]),
    forwardRef(() => TrackingModule),
    AuthModule,
  ],
  controllers: [CourierController],
  providers: [CourierService],
  exports: [CourierService],
})
export class CourierModule {}
