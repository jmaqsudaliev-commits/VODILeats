import { Module, forwardRef } from '@nestjs/common';
import { TrackingGateway } from './tracking.gateway';
import { OrdersModule } from '../orders/orders.module';
import { DispatcherModule } from '../dispatcher/dispatcher.module';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [
    AuthModule,
    forwardRef(() => OrdersModule),
    forwardRef(() => DispatcherModule),
  ],
  providers: [TrackingGateway],
  exports: [TrackingGateway],
})
export class TrackingModule {}
