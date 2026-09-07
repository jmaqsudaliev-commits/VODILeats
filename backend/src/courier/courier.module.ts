import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CourierService } from './courier.service';
import { CourierController } from './courier.controller';
import { CourierProfile } from './entities/courier-profile.entity';
import { TrackingModule } from '../tracking/tracking.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([CourierProfile]),
    forwardRef(() => TrackingModule),
  ],
  controllers: [CourierController],
  providers: [CourierService],
  exports: [CourierService],
})
export class CourierModule {}
