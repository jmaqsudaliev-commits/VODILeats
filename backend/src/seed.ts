import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { DataSource } from 'typeorm';
import { User, UserRole } from './users/entities/user.entity';
import { Restaurant } from './restaurants/entities/restaurant.entity';
import { Category } from './menu/entities/category.entity';
import { MenuItem } from './menu/entities/menu-item.entity';

async function seed() {
  const app = await NestFactory.createApplicationContext(AppModule);
  const dataSource = app.get(DataSource);

  console.log('🌱 Seeding VODIL EATS database...');

  const userRepo = dataSource.getRepository(User);
  const restaurantRepo = dataSource.getRepository(Restaurant);
  const categoryRepo = dataSource.getRepository(Category);
  const menuItemRepo = dataSource.getRepository(MenuItem);

  // 1. Create Users
  let owner = await userRepo.findOne({ where: { phone: '+998901112233' } });
  if (!owner) {
    owner = userRepo.create({
      phone: '+998901112233',
      firstName: 'Alisher',
      lastName: 'Usmonov',
      role: UserRole.RESTAURANT_OWNER,
      isPhoneVerified: true,
      isActive: true,
    });
    owner = await userRepo.save(owner);
  }

  let courier = await userRepo.findOne({ where: { phone: '+998902223344' } });
  if (!courier) {
    courier = userRepo.create({
      phone: '+998902223344',
      firstName: 'Bobur',
      lastName: 'Ergashev',
      role: UserRole.COURIER,
      isPhoneVerified: true,
      isActive: true,
    });
    courier = await userRepo.save(courier);
  }

  let customer = await userRepo.findOne({ where: { phone: '+998903334455' } });
  if (!customer) {
    customer = userRepo.create({
      phone: '+998903334455',
      firstName: 'Azizbek',
      lastName: 'Rahimov',
      role: UserRole.CUSTOMER,
      isPhoneVerified: true,
      isActive: true,
    });
    customer = await userRepo.save(customer);
  }

  // 2. Create Restaurants
  const restaurantsData = [
    {
      name: 'Shohona Milliy Taomlar',
      description: 'Eng sara Fargona va Vodil milliy taomlari: Osh, Manti, Shashlik',
      address: 'Vodil markazi, Mustaqillik shoh koʻchasi 14',
      latitude: 40.1795,
      longitude: 71.7250,
      phone: '+998732221100',
      rating: 4.9,
      totalReviews: 245,
      avgDeliveryTimeMinutes: 25,
      minimumOrderAmount: 20000,
      deliveryFeeBase: 5000,
      deliveryFeePerKm: 1200,
      imageUrl: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600',
      coverImageUrl: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=1200',
      ownerId: owner.id,
      categories: [
        {
          name: 'Asosiy Taomlar',
          items: [
            { name: 'To\'y Oshi (Devzira guruchda)', description: 'Qo\'y go\'shti, qizil va sariq sabzi, noxat, mayiz', price: 38000, imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400' },
            { name: 'Vodil Tandir Somsa', description: 'Mayin qatlama xamir, maydalangan mol go\'shti va dumba', price: 9000, imageUrl: 'https://images.unsplash.com/photo-1541544741938-0af808871cc0?w=400' },
            { name: 'Qo\'y Go\'shtidan Shashlik', description: 'Maxsus ziravorlar bilan marinadlangan 1 dona six', price: 18000, imageUrl: 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?w=400' },
          ]
        },
        {
          name: 'Salatlar & Ichimliklar',
          items: [
            { name: 'Achchiq-chuchuk', description: 'Pomidor, piyoz, achchiq qalampir', price: 8000, imageUrl: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400' },
            { name: 'Ko\'k Choy choynakda', description: 'Limon va novvot bilan', price: 5000, imageUrl: 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=400' }
          ]
        }
      ]
    },
    {
      name: 'Vodil Fast Food & Burger',
      description: 'Mazali lavashlar, burgerlar va crispy tovuq qanotlari',
      address: 'Vodil shoh koʻchasi 2',
      latitude: 40.1750,
      longitude: 71.7210,
      phone: '+998732228899',
      rating: 4.7,
      totalReviews: 180,
      avgDeliveryTimeMinutes: 20,
      minimumOrderAmount: 15000,
      deliveryFeeBase: 4000,
      deliveryFeePerKm: 1000,
      imageUrl: 'https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600',
      coverImageUrl: 'https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=1200',
      ownerId: owner.id,
      categories: [
        {
          name: 'Lavash & Donar',
          items: [
            { name: 'Klassik Lavash (Mol go\'shti)', description: 'Tandir go\'shti, pomidor, bodring, maxsus sous', price: 28000, imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400' },
            { name: 'Pishloqli Lavash', description: 'Eritilgan golland pishlog\'i va sershira go\'sht', price: 32000, imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400' },
            { name: 'Shaurma Tandir', description: 'Yangi qovurilgan kartoshka fri va go\'sht', price: 24000, imageUrl: 'https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=400' }
          ]
        }
      ]
    }
  ];

  for (const rData of restaurantsData) {
    let existing = await restaurantRepo.findOne({ where: { name: rData.name } });
    if (!existing) {
      const rest = restaurantRepo.create({
        name: rData.name,
        description: rData.description,
        address: rData.address,
        latitude: rData.latitude,
        longitude: rData.longitude,
        phone: rData.phone,
        rating: rData.rating,
        totalReviews: rData.totalReviews,
        avgDeliveryTimeMinutes: rData.avgDeliveryTimeMinutes,
        minimumOrderAmount: rData.minimumOrderAmount,
        deliveryFeeBase: rData.deliveryFeeBase,
        deliveryFeePerKm: rData.deliveryFeePerKm,
        imageUrl: rData.imageUrl,
        coverImageUrl: rData.coverImageUrl,
        ownerId: rData.ownerId,
        isActive: true,
        isVerified: true
      });
      const savedRest = await restaurantRepo.save(rest);

      for (const catData of rData.categories) {
        const cat = categoryRepo.create({
          name: catData.name,
          restaurantId: savedRest.id,
          sortOrder: 1,
          isActive: true
        });
        const savedCat = await categoryRepo.save(cat);

        for (const itemData of catData.items) {
          const item = menuItemRepo.create({
            name: itemData.name,
            description: itemData.description,
            price: itemData.price,
            imageUrl: itemData.imageUrl,
            categoryId: savedCat.id,
            isAvailable: true
          });
          await menuItemRepo.save(item);
        }
      }
    }
  }

  console.log('✅ Database seeded successfully!');
  await app.close();
}

seed().catch(err => {
  console.error('❌ Seeding failed:', err);
  process.exit(1);
});
