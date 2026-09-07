import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
  Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Category } from '../../menu/entities/category.entity';

@Entity('restaurants')
export class Restaurant {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 200 })
  @Index('IDX_RESTAURANT_NAME')
  name: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ length: 500 })
  address: string;

  @Column('float')
  latitude: number;

  @Column('float')
  longitude: number;

  @Column({ length: 500, nullable: true })
  imageUrl: string;

  @Column({ length: 500, nullable: true })
  coverImageUrl: string;

  @Column({ length: 20 })
  phone: string;

  @Column({ type: 'decimal', precision: 2, scale: 1, default: 0 })
  rating: number;

  @Column({ default: 0 })
  totalReviews: number;

  @Column({ type: 'int', default: 30 })
  avgDeliveryTimeMinutes: number;

  @Column({ type: 'decimal', precision: 10, scale: 2, default: 0 })
  minimumOrderAmount: number;

  @Column({ type: 'decimal', precision: 10, scale: 2, default: 5000 })
  deliveryFeeBase: number;

  @Column({ type: 'decimal', precision: 10, scale: 2, default: 1000 })
  deliveryFeePerKm: number;

  @Column({ default: '09:00' })
  openTime: string;

  @Column({ default: '23:00' })
  closeTime: string;

  @Column({ default: true })
  isActive: boolean;

  @Column({ default: false })
  isVerified: boolean;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'ownerId' })
  owner: User;

  @Column('uuid')
  @Index('IDX_RESTAURANT_OWNER')
  ownerId: string;

  @OneToMany(() => Category, (category) => category.restaurant, { cascade: true })
  categories: Category[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
