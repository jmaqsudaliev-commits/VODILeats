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
import { Restaurant } from '../../restaurants/entities/restaurant.entity';
import { CourierProfile } from '../../courier/entities/courier-profile.entity';
import { OrderItem } from './order-item.entity';

export enum OrderStatus {
  PENDING = 'pending',
  CONFIRMED = 'confirmed',
  PREPARING = 'preparing',
  READY_FOR_PICKUP = 'ready_for_pickup',
  COURIER_ASSIGNED = 'courier_assigned',
  COURIER_PICKING_UP = 'courier_picking_up',
  COURIER_PICKED_UP = 'courier_picked_up',
  DELIVERING = 'delivering',
  DELIVERED = 'delivered',
  CANCELLED = 'cancelled',
}

export enum PaymentMethod {
  CASH = 'cash',
  CARD = 'card',
}

@Entity('orders')
export class Order {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true, length: 20 })
  @Index('IDX_ORDER_NUMBER')
  orderNumber: string;

  @Column({
    type: 'simple-enum',
    enum: OrderStatus,
    default: OrderStatus.PENDING,
  })
  @Index('IDX_ORDER_STATUS')
  status: OrderStatus;

  @Column({
    type: 'simple-enum',
    enum: PaymentMethod,
    default: PaymentMethod.CASH,
  })
  paymentMethod: PaymentMethod;

  // Mijoz ma'lumotlari
  @ManyToOne(() => User, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'customerId' })
  customer: User;

  @Column('uuid')
  @Index('IDX_ORDER_CUSTOMER')
  customerId: string;

  // Restoran ma'lumotlari
  @ManyToOne(() => Restaurant, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'restaurantId' })
  restaurant: Restaurant;

  @Column('uuid')
  @Index('IDX_ORDER_RESTAURANT')
  restaurantId: string;

  // Kuryer ma'lumotlari
  @ManyToOne(() => CourierProfile, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'courierId' })
  courier: CourierProfile;

  @Column({ type: 'uuid', nullable: true })
  @Index('IDX_ORDER_COURIER')
  courierId: string;

  // Yetkazish manzili
  @Column({ length: 500 })
  deliveryAddress: string;

  @Column('float')
  deliveryLatitude: number;

  @Column('float')
  deliveryLongitude: number;

  // Narxlar
  @Column({ type: 'decimal', precision: 12, scale: 2, default: 0 })
  subtotal: number;

  @Column({ type: 'decimal', precision: 12, scale: 2, default: 0 })
  deliveryFee: number;

  @Column({ type: 'decimal', precision: 12, scale: 2, default: 0 })
  totalAmount: number;

  @Column({ type: 'decimal', precision: 10, scale: 2, default: 0 })
  distanceKm: number;

  // Vaqtlar
  @Column({ nullable: true })
  estimatedDeliveryTime: Date;

  @Column({ nullable: true })
  confirmedAt: Date;

  @Column({ nullable: true })
  preparingAt: Date;

  @Column({ nullable: true })
  readyAt: Date;

  @Column({ nullable: true })
  pickedUpAt: Date;

  @Column({ nullable: true })
  deliveredAt: Date;

  @Column({ nullable: true })
  cancelledAt: Date;

  @Column({ nullable: true, type: 'text' })
  cancellationReason: string;

  @Column({ nullable: true, type: 'text' })
  customerNote: string;

  // Buyurtma tarkibi
  @OneToMany(() => OrderItem, (item) => item.order, { cascade: true, eager: true })
  items: OrderItem[];

  @CreateDateColumn()
  @Index('IDX_ORDER_CREATED')
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
