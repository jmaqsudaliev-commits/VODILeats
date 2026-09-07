import {
  Controller,
  Get,
  Post,
  Put,
  Param,
  Body,
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
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('courier')
export class CourierController {
  constructor(private readonly courierService: CourierService) {}

  @Post('profile')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Kuryer profilini yaratish' })
  async createProfile(@Request() req: any, @Body() data: any) {
    return this.courierService.createProfile(req.user.id, data);
  }

  @Get('profile')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Kuryer profilini olish' })
  async getProfile(@Request() req: any) {
    return this.courierService.getProfile(req.user.id);
  }

  @Put('status')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Statusni o\'zgartirish (online/offline)' })
  async updateStatus(
    @Request() req: any,
    @Body('status') status: CourierStatus,
  ) {
    return this.courierService.updateStatus(req.user.id, status);
  }

  @Put('location')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
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
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Buyurtmani qabul qilish' })
  async acceptOrder(
    @Request() req: any,
    @Param('orderId') orderId: string,
  ) {
    return this.courierService.acceptOrder(req.user.id, orderId);
  }

  @Put('orders/:orderId/reject')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Buyurtmani rad etish' })
  async rejectOrder(
    @Request() req: any,
    @Param('orderId') orderId: string,
  ) {
    await this.courierService.rejectOrder(req.user.id, orderId);
    return { message: 'Buyurtma rad etildi' };
  }

  @Put('delivery/complete')
  @Roles(UserRole.COURIER)
  @UseGuards(RolesGuard)
  @ApiOperation({ summary: 'Yetkazishni yakunlash' })
  async completeDelivery(@Request() req: any) {
    return this.courierService.completeDelivery(req.user.id);
  }
}
