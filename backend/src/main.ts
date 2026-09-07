import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { IoAdapter } from '@nestjs/platform-socket.io';
const compression = require('compression');

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    logger: ['error', 'warn', 'log'],
    bufferLogs: true,
  });

  // Enable Gzip/Brotli payload compression for 1M+ user bandwidth saving
  app.use(compression({
    level: 6,
    threshold: 1024, // only compress responses above 1KB
  }));

  // Enable graceful shutdown for zero-downtime updates
  app.enableShutdownHooks();

  app.enableCors({
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE,OPTIONS',
    credentials: true,
  });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: {
        enableImplicitConversion: true,
      },
    }),
  );

  const path = require('path');
  const fs = require('fs');
  const express = require('express');
  const candidates = [
    path.resolve(__dirname, '..', '..', 'ADMIN_PANEL'),
    path.resolve(__dirname, '..', 'ADMIN_PANEL'),
    path.resolve(process.cwd(), 'ADMIN_PANEL'),
    path.resolve(process.cwd(), '..', 'ADMIN_PANEL'),
  ];
  const adminStaticPath = candidates.find((p: string) => fs.existsSync(p)) || candidates[0];
  app.use('/admin', express.static(adminStaticPath));

  const httpAdapter = app.getHttpAdapter();
  httpAdapter.get('/', (req: any, res: any) => {
    res.redirect('/admin');
  });

  app.setGlobalPrefix('api/v1');

  app.useWebSocketAdapter(new IoAdapter(app));

  const config = new DocumentBuilder()
    .setTitle('VODIL EATS API — High-Load Architecture')
    .setDescription('Production-ready delivery platform API configured for 1,000,000+ users')
    .setVersion('1.0')
    .addBearerAuth()
    .addTag('auth', 'Authentication & Authorization')
    .addTag('users', 'User Management')
    .addTag('restaurants', 'Restaurant Management')
    .addTag('menu', 'Menu & Categories')
    .addTag('orders', 'Order Management')
    .addTag('courier', 'Courier Operations')
    .addTag('tracking', 'Live Tracking')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  const port = process.env.PORT || 3000;
  await app.listen(port, '0.0.0.0');

  console.log(`\n============================================================`);
  console.log(`🚀 VODIL EATS HIGH-SCALE API IS RUNNING!`);
  console.log(`📡 Local API:     http://localhost:${port}/api/v1`);
  console.log(`📚 Swagger Docs:  http://localhost:${port}/api/docs`);
  console.log(`⚡ WebSocket URL: ws://localhost:${port}`);
  console.log(`📈 Scale Target: 1,000,000+ Concurrent Ready Architecture`);
  console.log(`============================================================\n`);
}

bootstrap();
