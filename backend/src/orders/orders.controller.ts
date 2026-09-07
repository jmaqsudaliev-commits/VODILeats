import {
  Controller,
  Get,
  Post,
  Put,
  Param,
  Body,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { OrdersService, CreateOrderDto } from './orders.service';
import { OrderStatus } from './entities/order.entity';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard, Roles } from '../auth/guards/roles.guard';
import { UserRole } from '../users/entities/user.entity';

@ApiTags('orders')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  // ==================== Mijoz endpointlari ====================

  @Post()
  @Roles(UserRole.CUSTOMER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Yangi buyurtma yaratish' })
  async createOrder(@Request() req: any, @Body() dto: CreateOrderDto) {
    return this.ordersService.createOrder(req.user.id, dto);
  }

  @Get('my-orders')
  @Roles(UserRole.CUSTOMER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Mening buyurtmalarim' })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  async getMyOrders(
    @Request() req: any,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.ordersService.getCustomerOrders(req.user.id, page, limit);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Buyurtma detallari' })
  async getOrder(@Param('id') id: string) {
    return this.ordersService.findById(id);
  }

  @Put(':id/cancel')
  @ApiOperation({ summary: 'Buyurtmani bekor qilish' })
  async cancelOrder(
    @Param('id') id: string,
    @Body('reason') reason: string,
  ) {
    return this.ordersService.cancelOrder(id, reason);
  }

  // ==================== Restoran endpointlari ====================

  @Get('restaurant/:restaurantId')
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Restoran buyurtmalari' })
  @ApiQuery({ name: 'status', required: false, enum: OrderStatus })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  async getRestaurantOrders(
    @Param('restaurantId') restaurantId: string,
    @Query('status') status?: OrderStatus,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.ordersService.getRestaurantOrders(
      restaurantId,
      status,
      page,
      limit,
    );
  }

  @Get('restaurant/:restaurantId/active')
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Restoran faol buyurtmalari' })
  async getActiveOrders(@Param('restaurantId') restaurantId: string) {
    return this.ordersService.getActiveOrders(restaurantId);
  }

  @Put(':id/confirm')
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Buyurtmani tasdiqlash (restoran)' })
  async confirmOrder(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.CONFIRMED);
  }

  @Put(':id/preparing')
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Tayyorlash boshlandi' })
  async startPreparing(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.PREPARING);
  }

  @Put(':id/ready')
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Kuryerga topshirishga tayyor' })
  async markReady(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.READY_FOR_PICKUP);
  }

  // ==================== Kuryer endpointlari ====================

  @Get('courier/my-deliveries')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Kuryer buyurtmalari' })
  @ApiQuery({ name: 'status', required: false, enum: OrderStatus })
  async getCourierOrders(
    @Request() req: any,
    @Query('status') status?: OrderStatus,
  ) {
    return this.ordersService.getCourierOrders(req.user.id, status);
  }

  @Put(':id/picked-up')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Buyurtma restorandan olindi' })
  async markPickedUp(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.COURIER_PICKED_UP);
  }

  @Put(':id/delivering')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Yetkazish boshlandi' })
  async startDelivering(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.DELIVERING);
  }

  @Put(':id/delivered')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Buyurtma yetkazildi' })
  async markDelivered(@Param('id') id: string) {
    return this.ordersService.updateStatus(id, OrderStatus.DELIVERED);
  }
}
