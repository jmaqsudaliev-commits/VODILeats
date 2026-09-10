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
import { RestaurantsService } from './restaurants.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard, Roles } from '../auth/guards/roles.guard';
import { UserRole } from '../users/entities/user.entity';

@ApiTags('restaurants')
@Controller('restaurants')
export class RestaurantsController {
  constructor(private readonly restaurantsService: RestaurantsService) {}

  @Post('login-by-code')
  @ApiOperation({ summary: 'Restoran maxsus kodi orqali kirish' })
  async loginByCode(@Body('accessCode') accessCode: string) {
    return this.restaurantsService.loginByCode(accessCode);
  }

  @Put(':id/toggle-open')
  @ApiOperation({ summary: 'Restoran ochiq/yopiq holatini o\'zgartirish' })
  async toggleOpen(@Param('id') id: string) {
    return this.restaurantsService.toggleOpen(id);
  }

  @Get()
  @ApiOperation({ summary: 'Barcha restoranlarni olish (filtrlash bilan)' })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'latitude', required: false })
  @ApiQuery({ name: 'longitude', required: false })
  @ApiQuery({ name: 'maxDistanceKm', required: false })
  async findAll(
    @Query('search') search?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('latitude') latitude?: number,
    @Query('longitude') longitude?: number,
    @Query('maxDistanceKm') maxDistanceKm?: number,
  ) {
    return this.restaurantsService.findAll({
      search,
      page,
      limit,
      latitude,
      longitude,
      maxDistanceKm,
    });
  }

  @Get(':id')
  @ApiOperation({ summary: 'Restoran detallari (menyu bilan)' })
  async findById(@Param('id') id: string) {
    return this.restaurantsService.findById(id);
  }

  @Get(':id/delivery-fee')
  @ApiOperation({ summary: 'Yetkazish narxini hisoblash' })
  @ApiQuery({ name: 'latitude', required: true })
  @ApiQuery({ name: 'longitude', required: true })
  async calculateDeliveryFee(
    @Param('id') id: string,
    @Query('latitude') latitude: number,
    @Query('longitude') longitude: number,
  ) {
    const restaurant = await this.restaurantsService.findById(id);
    return this.restaurantsService.calculateDeliveryFee(
      restaurant,
      latitude,
      longitude,
    );
  }

  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Yangi restoran yaratish' })
  async create(@Request() req: any, @Body() data: any) {
    return this.restaurantsService.create(req.user.id, data);
  }

  @Put(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Restoran ma\'lumotlarini yangilash' })
  async update(
    @Param('id') id: string,
    @Request() req: any,
    @Body() data: any,
  ) {
    return this.restaurantsService.update(id, req.user.id, data);
  }

  @Get('owner/my-restaurants')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Mening restoranlarim' })
  async myRestaurants(@Request() req: any) {
    return this.restaurantsService.findByOwner(req.user.id);
  }
}
