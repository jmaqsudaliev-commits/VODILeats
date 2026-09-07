import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Category } from './entities/category.entity';
import { MenuItem } from './entities/menu-item.entity';

@Injectable()
export class MenuService {
  constructor(
    @InjectRepository(Category)
    private readonly categoryRepository: Repository<Category>,
    @InjectRepository(MenuItem)
    private readonly menuItemRepository: Repository<MenuItem>,
  ) {}

  // ==================== Kategoriyalar ====================

  async getCategories(restaurantId: string): Promise<Category[]> {
    return this.categoryRepository.find({
      where: { restaurantId, isActive: true },
      relations: ['items'],
      order: { sortOrder: 'ASC' },
    });
  }

  async createCategory(
    restaurantId: string,
    data: Partial<Category>,
  ): Promise<Category> {
    const category = this.categoryRepository.create({
      ...data,
      restaurantId,
    });
    return this.categoryRepository.save(category);
  }

  async updateCategory(
    categoryId: string,
    data: Partial<Category>,
  ): Promise<Category> {
    const category = await this.categoryRepository.findOne({
      where: { id: categoryId },
    });
    if (!category) {
      throw new NotFoundException('Kategoriya topilmadi');
    }
    Object.assign(category, data);
    return this.categoryRepository.save(category);
  }

  async deleteCategory(categoryId: string): Promise<void> {
    const result = await this.categoryRepository.delete(categoryId);
    if (result.affected === 0) {
      throw new NotFoundException('Kategoriya topilmadi');
    }
  }

  // ==================== Menyu elementlari ====================

  async getMenuItems(categoryId: string): Promise<MenuItem[]> {
    return this.menuItemRepository.find({
      where: { categoryId, isAvailable: true },
      order: { sortOrder: 'ASC' },
    });
  }

  async getMenuItemById(id: string): Promise<MenuItem> {
    const item = await this.menuItemRepository.findOne({
      where: { id },
    });
    if (!item) {
      throw new NotFoundException('Menyu elementi topilmadi');
    }
    return item;
  }

  async createMenuItem(
    categoryId: string,
    data: Partial<MenuItem>,
  ): Promise<MenuItem> {
    const category = await this.categoryRepository.findOne({
      where: { id: categoryId },
    });
    if (!category) {
      throw new NotFoundException('Kategoriya topilmadi');
    }

    const item = this.menuItemRepository.create({
      ...data,
      categoryId,
    });
    return this.menuItemRepository.save(item);
  }

  async updateMenuItem(
    itemId: string,
    data: Partial<MenuItem>,
  ): Promise<MenuItem> {
    const item = await this.menuItemRepository.findOne({
      where: { id: itemId },
    });
    if (!item) {
      throw new NotFoundException('Menyu elementi topilmadi');
    }
    Object.assign(item, data);
    return this.menuItemRepository.save(item);
  }

  async deleteMenuItem(itemId: string): Promise<void> {
    const result = await this.menuItemRepository.delete(itemId);
    if (result.affected === 0) {
      throw new NotFoundException('Menyu elementi topilmadi');
    }
  }

  /**
   * Stop-listga qo'shish/olib tashlash
   */
  async toggleStopList(itemId: string): Promise<MenuItem> {
    const item = await this.menuItemRepository.findOne({
      where: { id: itemId },
    });
    if (!item) {
      throw new NotFoundException('Menyu elementi topilmadi');
    }
    item.isInStopList = !item.isInStopList;
    item.isAvailable = !item.isInStopList;
    return this.menuItemRepository.save(item);
  }

  /**
   * Restoranning to'liq menyusini olish
   */
  async getFullMenu(restaurantId: string): Promise<Category[]> {
    return this.categoryRepository.find({
      where: { restaurantId, isActive: true },
      relations: ['items'],
      order: { sortOrder: 'ASC' },
    });
  }
}
