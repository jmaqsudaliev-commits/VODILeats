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
import { Category } from './category.entity';

@Entity('menu_items')
export class MenuItem {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 200 })
  @Index('IDX_MENU_ITEM_NAME')
  name: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ type: 'decimal', precision: 10, scale: 2 })
  price: number;

  @Column({ nullable: true, length: 500 })
  imageUrl: string;

  @Column({ type: 'int', default: 0 })
  preparationTimeMinutes: number;

  @Column({ type: 'int', default: 0 })
  calories: number;

  @Column({ type: 'simple-array', nullable: true })
  tags: string[]; // "spicy", "vegetarian", "halal"

  @Column({ default: true })
  isAvailable: boolean;

  @Column({ default: false })
  @Index('IDX_MENU_ITEM_STOP_LIST')
  isInStopList: boolean;

  @Column({ type: 'int', default: 0 })
  sortOrder: number;

  @ManyToOne(() => Category, (category) => category.items, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'categoryId' })
  category: Category;

  @Column('uuid')
  categoryId: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
