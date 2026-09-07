import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Param,
  Body,
  UseGuards,
  Patch,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { MenuService } from './menu.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard, Roles } from '../auth/guards/roles.guard';
import { UserRole } from '../users/entities/user.entity';

@ApiTags('menu')
@Controller('menu')
export class MenuController {
  constructor(private readonly menuService: MenuService) {}

  // ==================== Ommaviy (public) endpointlar ====================

  @Get('restaurant/:restaurantId')
  @ApiOperation({ summary: 'Restoran menyusini olish' })
  async getFullMenu(@Param('restaurantId') restaurantId: string) {
    return this.menuService.getFullMenu(restaurantId);
  }

  @Get('categories/:restaurantId')
  @ApiOperation({ summary: 'Restoran kategoriyalarini olish' })
  async getCategories(@Param('restaurantId') restaurantId: string) {
    return this.menuService.getCategories(restaurantId);
  }

  @Get('items/:categoryId')
  @ApiOperation({ summary: 'Kategoriya elementlarini olish' })
  async getMenuItems(@Param('categoryId') categoryId: string) {
    return this.menuService.getMenuItems(categoryId);
  }

  @Get('item/:id')
  @ApiOperation({ summary: 'Menyu elementi detallari' })
  async getMenuItem(@Param('id') id: string) {
    return this.menuService.getMenuItemById(id);
  }

  // ==================== Restaurant owner endpointlari ====================

  @Post('categories/:restaurantId')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Yangi kategoriya yaratish' })
  async createCategory(
    @Param('restaurantId') restaurantId: string,
    @Body() data: any,
  ) {
    return this.menuService.createCategory(restaurantId, data);
  }

  @Put('categories/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Kategoriyani yangilash' })
  async updateCategory(@Param('id') id: string, @Body() data: any) {
    return this.menuService.updateCategory(id, data);
  }

  @Delete('categories/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Kategoriyani o\'chirish' })
  async deleteCategory(@Param('id') id: string) {
    await this.menuService.deleteCategory(id);
    return { message: 'Kategoriya o\'chirildi' };
  }

  @Post('items/:categoryId')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Yangi menyu elementi qo\'shish' })
  async createMenuItem(
    @Param('categoryId') categoryId: string,
    @Body() data: any,
  ) {
    return this.menuService.createMenuItem(categoryId, data);
  }

  @Put('items/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Menyu elementini yangilash' })
  async updateMenuItem(@Param('id') id: string, @Body() data: any) {
    return this.menuService.updateMenuItem(id, data);
  }

  @Delete('items/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Menyu elementini o\'chirish' })
  async deleteMenuItem(@Param('id') id: string) {
    await this.menuService.deleteMenuItem(id);
    return { message: 'Menyu elementi o\'chirildi' };
  }

  @Patch('items/:id/stop-list')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles(UserRole.RESTAURANT_OWNER, UserRole.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Stop-listga qo\'shish/olib tashlash' })
  async toggleStopList(@Param('id') id: string) {
    return this.menuService.toggleStopList(id);
  }
}
