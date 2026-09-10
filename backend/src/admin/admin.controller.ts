import {
  Controller,
  Get,
  Put,
  Post,
  Delete,
  Param,
  Query,
  Body,
  UnauthorizedException,
} from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { OrderStatus } from '../orders/entities/order.entity';
import { CourierStatus } from '../courier/entities/courier-profile.entity';

@ApiTags('admin')
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Post('auth/login')
  @ApiOperation({ summary: 'Admin 20 talik paroli orqali kirish' })
  async adminLogin(@Body('password') password: string) {
    if (this.adminService.verifyAdminKey(password)) {
      return {
        success: true,
        token: 'vodil_admin_authorized_2026',
        message: 'Hush kelibsiz, platforma egasi!',
      };
    }
    throw new UnauthorizedException('Xavfsizlik paroli noto\'g\'ri! Faqat ilova egasiga ruxsat berilgan.');
  }

  @Get('stats')
  @ApiOperation({ summary: 'Boshqaruv paneli umumiy statistikasi' })
  async getStats() {
    return this.adminService.getStats();
  }

  @Get('orders')
  @ApiOperation({ summary: 'Barcha buyurtmalar ro\'yxati' })
  async getAllOrders(
    @Query('status') status?: string,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getAllOrders(status, limit);
  }

  @Get('restaurants')
  @ApiOperation({ summary: 'Barcha restoranlar ro\'yxati' })
  async getAllRestaurants() {
    return this.adminService.getAllRestaurants();
  }

  @Post('restaurants')
  @ApiOperation({ summary: 'Yangi restoran qo\'shish' })
  async createRestaurant(@Body() body: any) {
    return this.adminService.createRestaurant(body);
  }

  @Put('restaurants/:id/toggle')
  @ApiOperation({ summary: 'Restoran faolligini o\'zgartirish' })
  async toggleRestaurantStatus(
    @Param('id') id: string,
    @Body('field') field: 'isActive' | 'isVerified' = 'isActive',
  ) {
    return this.adminService.toggleRestaurantStatus(id, field);
  }

  @Delete('restaurants/:id')
  @ApiOperation({ summary: 'Restoranni o\'chirish' })
  async deleteRestaurant(@Param('id') id: string) {
    return this.adminService.deleteRestaurant(id);
  }

  @Get('couriers')
  @ApiOperation({ summary: 'Barcha kuryerlar ro\'yxati va geolokatsiyasi' })
  async getAllCouriers() {
    return this.adminService.getAllCouriers();
  }

  @Post('couriers')
  @ApiOperation({ summary: 'Yangi kuryer qo\'shish' })
  async createCourier(@Body() body: any) {
    return this.adminService.createCourier(body);
  }

  @Put('couriers/:id/verify')
  @ApiOperation({ summary: 'Kuryerni admin tomonidan tasdiqlash yoki bekor qilish' })
  async verifyCourier(
    @Param('id') id: string,
    @Body('isVerified') isVerified?: boolean,
  ) {
    return this.adminService.verifyCourier(id, isVerified !== false);
  }

  @Post('restaurants/:id/regenerate-code')
  @ApiOperation({ summary: 'Restoran maxsus kodini qayta generatsiya qilish' })
  async regenerateRestaurantCode(@Param('id') id: string) {
    return this.adminService.regenerateRestaurantCode(id);
  }

  @Put('couriers/:id/status')
  @ApiOperation({ summary: 'Kuryer statusini o\'zgartirish' })
  async updateCourierStatus(
    @Param('id') id: string,
    @Body('status') status: CourierStatus,
  ) {
    return this.adminService.updateCourierStatus(id, status);
  }

  @Get('customers')
  @ApiOperation({ summary: 'Barcha mijozlar ro\'yxati' })
  async getAllCustomers() {
    return this.adminService.getAllCustomers();
  }

  @Put('orders/:id/status')
  @ApiOperation({ summary: 'Buyurtma statusini o\'zgartirish (Admin)' })
  async forceUpdateOrderStatus(
    @Param('id') id: string,
    @Body('status') status: OrderStatus,
  ) {
    return this.adminService.forceUpdateOrderStatus(id, status);
  }

  @Post('orders/demo')
  @ApiOperation({ summary: 'Jonli sinash uchun demo buyurtma yaratish' })
  async createDemoOrder() {
    return this.adminService.createDemoOrder();
  }
}
