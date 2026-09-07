import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { Address } from './entities/address.entity';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(Address)
    private readonly addressRepository: Repository<Address>,
  ) {}

  async findById(id: string): Promise<User> {
    const user = await this.userRepository.findOne({
      where: { id },
      relations: ['addresses'],
    });
    if (!user) {
      throw new NotFoundException('Foydalanuvchi topilmadi');
    }
    return user;
  }

  async findByPhone(phone: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { phone } });
  }

  async updateProfile(
    userId: string,
    data: Partial<Pick<User, 'firstName' | 'lastName' | 'avatarUrl'>>,
  ): Promise<User> {
    await this.userRepository.update(userId, data);
    return this.findById(userId);
  }

  // ==================== Manzillar ====================

  async getAddresses(userId: string): Promise<Address[]> {
    return this.addressRepository.find({
      where: { userId },
      order: { isDefault: 'DESC', createdAt: 'DESC' },
    });
  }

  async addAddress(
    userId: string,
    data: Omit<Address, 'id' | 'userId' | 'user' | 'createdAt' | 'updatedAt'>,
  ): Promise<Address> {
    if (data.isDefault) {
      await this.addressRepository.update(
        { userId },
        { isDefault: false },
      );
    }

    const address = this.addressRepository.create({
      ...data,
      userId,
    });
    return this.addressRepository.save(address);
  }

  async updateAddress(
    userId: string,
    addressId: string,
    data: Partial<Address>,
  ): Promise<Address> {
    const address = await this.addressRepository.findOne({
      where: { id: addressId, userId },
    });
    if (!address) {
      throw new NotFoundException('Manzil topilmadi');
    }

    if (data.isDefault) {
      await this.addressRepository.update(
        { userId },
        { isDefault: false },
      );
    }

    Object.assign(address, data);
    return this.addressRepository.save(address);
  }

  async deleteAddress(userId: string, addressId: string): Promise<void> {
    const result = await this.addressRepository.delete({
      id: addressId,
      userId,
    });
    if (result.affected === 0) {
      throw new NotFoundException('Manzil topilmadi');
    }
  }
}
