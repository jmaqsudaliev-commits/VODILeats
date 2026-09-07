import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
  Index,
} from 'typeorm';
import { User } from './user.entity';

@Entity('addresses')
export class Address {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 255 })
  title: string; // "Uy", "Ish", etc.

  @Column({ length: 500 })
  street: string;

  @Column({ nullable: true, length: 100 })
  apartment: string;

  @Column({ nullable: true, length: 100 })
  entrance: string;

  @Column({ nullable: true, length: 50 })
  floor: string;

  @Column({ nullable: true, length: 500 })
  comment: string;

  @Column('float')
  @Index('IDX_ADDRESS_LAT')
  latitude: number;

  @Column('float')
  @Index('IDX_ADDRESS_LNG')
  longitude: number;

  @Column({ default: false })
  isDefault: boolean;

  @ManyToOne(() => User, (user) => user.addresses, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user: User;

  @Column('uuid')
  userId: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
