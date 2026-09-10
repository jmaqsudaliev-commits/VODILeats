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
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { CourierService } from './courier.service';
import { CourierStatus } from './entities/courier-profile.entity';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard, Roles } from '../auth/guards/roles.guard';
import { UserRole } from '../users/entities/user.entity';

@ApiTags('courier')
@Controller('courier')
export class CourierController {
  constructor(private readonly courierService: CourierService) {}

  @Post('register')
  @ApiOperation({ summary: 'Kuryer ro\'yxatdan o\'tishi (Admin tasdig\'i talab etiladi)' })
  async register(@Body() body: any) {
    return this.courierService.register(body);
  }

  @Post('login')
  @ApiOperation({ summary: 'Kuryer login (Telefon + Parol)' })
  async login(@Body() body: any) {
    return this.courierService.login(body);
  }

  @Get('status-check')
  @ApiOperation({ summary: 'Kuryer tasdiqlanganlik holatini tekshirish' })
  async checkStatus(@Query('phone') phone: string) {
    return this.courierService.checkStatus(phone);
  }

  @Post('profile')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Kuryer profilini yaratish' })
  async createProfile(@Request() req: any, @Body() data: any) {
    return this.courierService.createProfile(req.user.id, data);
  }

  @Get('profile')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Kuryer profilini olish' })
  async getProfile(@Request() req: any) {
    return this.courierService.getProfile(req.user.id);
  }

  @Put('status')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Statusni o\'zgartirish (online/offline)' })
  async updateStatus(
    @Request() req: any,
    @Body('status') status: CourierStatus,
  ) {
    return this.courierService.updateStatus(req.user.id, status);
  }

  @Put('location')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'GPS lokatsiyani yangilash' })
  async updateLocation(
    @Request() req: any,
    @Body() data: { latitude: number; longitude: number },
  ) {
    await this.courierService.updateLocation(
      req.user.id,
      data.latitude,
      data.longitude,
    );
    return { message: 'Lokatsiya yangilandi' };
  }

  @Put('orders/:orderId/accept')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Buyurtmani qabul qilish' })
  async acceptOrder(
    @Request() req: any,
    @Param('orderId') orderId: string,
  ) {
    return this.courierService.acceptOrder(req.user.id, orderId);
  }

  @Put('orders/:orderId/reject')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Buyurtmani rad etish' })
  async rejectOrder(
    @Request() req: any,
    @Param('orderId') orderId: string,
  ) {
    await this.courierService.rejectOrder(req.user.id, orderId);
    return { message: 'Buyurtma rad etildi' };
  }

  @Put('delivery/complete')
  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.COURIER)
  @ApiOperation({ summary: 'Yetkazishni yakunlash' })
  async completeDelivery(@Request() req: any) {
    return this.courierService.completeDelivery(req.user.id);
  }
}
