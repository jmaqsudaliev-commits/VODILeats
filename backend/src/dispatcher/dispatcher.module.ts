import { Module, forwardRef } from '@nestjs/common';
import { DispatcherService } from './dispatcher.service';
import { OrdersModule } from '../orders/orders.module';
import { CourierModule } from '../courier/courier.module';
import { TrackingModule } from '../tracking/tracking.module';

@Module({
  imports: [
    forwardRef(() => OrdersModule),
    forwardRef(() => CourierModule),
    forwardRef(() => TrackingModule),
  ],
  providers: [DispatcherService],
  exports: [DispatcherService],
})
export class DispatcherModule {}
