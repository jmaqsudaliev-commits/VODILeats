import {
  Controller,
  Get,
  Put,
  Post,
  Delete,
  Body,
  Param,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UsersService } from './users.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@ApiTags('users')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('profile')
  @ApiOperation({ summary: 'Joriy foydalanuvchi profilini olish' })
  async getProfile(@Request() req: any) {
    return this.usersService.findById(req.user.id);
  }

  @Put('profile')
  @ApiOperation({ summary: 'Profilni yangilash' })
  async updateProfile(
    @Request() req: any,
    @Body() data: { firstName?: string; lastName?: string; avatarUrl?: string },
  ) {
    return this.usersService.updateProfile(req.user.id, data);
  }

  @Get('addresses')
  @ApiOperation({ summary: 'Foydalanuvchi manzillarini olish' })
  async getAddresses(@Request() req: any) {
    return this.usersService.getAddresses(req.user.id);
  }

  @Post('addresses')
  @ApiOperation({ summary: 'Yangi manzil qo\'shish' })
  async addAddress(@Request() req: any, @Body() data: any) {
    return this.usersService.addAddress(req.user.id, data);
  }

  @Put('addresses/:id')
  @ApiOperation({ summary: 'Manzilni yangilash' })
  async updateAddress(
    @Request() req: any,
    @Param('id') addressId: string,
    @Body() data: any,
  ) {
    return this.usersService.updateAddress(req.user.id, addressId, data);
  }

  @Delete('addresses/:id')
  @ApiOperation({ summary: 'Manzilni o\'chirish' })
  async deleteAddress(@Request() req: any, @Param('id') addressId: string) {
    await this.usersService.deleteAddress(req.user.id, addressId);
    return { message: 'Manzil o\'chirildi' };
  }
}
