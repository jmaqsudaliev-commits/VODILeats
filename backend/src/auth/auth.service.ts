import {
  Injectable,
  UnauthorizedException,
  BadRequestException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { User, UserRole } from '../users/entities/user.entity';
import { RedisService } from '../redis/redis.service';
import { VerifyOtpDto, LoginPasswordDto } from './dto/auth.dto';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly redisService: RedisService,
  ) {}

  /**
   * SMS OTP jo'natish
   */
  async sendOtp(phone: string): Promise<{ message: string; otp?: string }> {
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    await this.redisService.setOtp(phone, otp);

    console.log(`📱 OTP for ${phone}: ${otp}`);

    return {
      message: 'OTP kodi yuborildi',
      otp: this.configService.get('NODE_ENV') === 'development' ? otp : undefined,
    };
  }

  /**
   * OTP'ni tekshirish va ro'yxatdan o'tish / parol o'rnatish
   */
  async verifyOtp(dto: VerifyOtpDto): Promise<{
    accessToken: string;
    refreshToken: string;
    user: Partial<User>;
    isNewUser: boolean;
  }> {
    const savedOtp = await this.redisService.getOtp(dto.phone);

    if (!savedOtp) {
      throw new BadRequestException('OTP kodi topilmadi yoki muddati o\'tgan');
    }

    if (savedOtp !== dto.otp) {
      throw new UnauthorizedException('Noto\'g\'ri OTP kodi');
    }

    await this.redisService.deleteOtp(dto.phone);

    let user = await this.userRepository.findOne({
      where: { phone: dto.phone },
    });

    let isNewUser = false;
    let passwordHash: string | undefined = undefined;
    if (dto.password) {
      passwordHash = await bcrypt.hash(dto.password, 10);
    }

    if (!user) {
      isNewUser = true;
      user = this.userRepository.create({
        phone: dto.phone,
        firstName: dto.firstName || 'Foydalanuvchi',
        lastName: dto.lastName || '',
        role: dto.role || UserRole.CUSTOMER,
        passwordHash,
        isPhoneVerified: true,
        isActive: true,
      });
      user = await this.userRepository.save(user);
    } else {
      user.isPhoneVerified = true;
      if (dto.firstName) user.firstName = dto.firstName;
      if (dto.lastName) user.lastName = dto.lastName;
      if (passwordHash) user.passwordHash = passwordHash;
      await this.userRepository.save(user);
    }

    const tokens = await this.generateTokens(user);
    const hashedRefreshToken = await bcrypt.hash(tokens.refreshToken, 10);
    await this.userRepository.update(user.id, {
      refreshToken: hashedRefreshToken,
    });

    return {
      ...tokens,
      user: {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone,
        role: user.role,
        isPhoneVerified: user.isPhoneVerified,
      },
      isNewUser,
    };
  }

  /**
   * Telefon raqam va parol orqali tizimga kirish (Login with Password)
   */
  async loginWithPassword(dto: LoginPasswordDto): Promise<{
    accessToken: string;
    refreshToken: string;
    user: Partial<User>;
  }> {
    const user = await this.userRepository.findOne({
      where: { phone: dto.phone },
      select: [
        'id',
        'phone',
        'firstName',
        'lastName',
        'role',
        'isPhoneVerified',
        'isActive',
        'passwordHash',
      ],
    });

    if (!user) {
      throw new UnauthorizedException('Telefon raqami yoki parol noto\'g\'ri');
    }

    if (!user.passwordHash) {
      throw new BadRequestException(
        'Ushbu hisobga hali parol o\'rnatilmagan. Iltimos, SMS kod orqali kiring va parol yarating.',
      );
    }

    const isPasswordValid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Telefon raqami yoki parol noto\'g\'ri');
    }

    const tokens = await this.generateTokens(user);
    const hashedRefreshToken = await bcrypt.hash(tokens.refreshToken, 10);
    await this.userRepository.update(user.id, {
      refreshToken: hashedRefreshToken,
    });

    return {
      ...tokens,
      user: {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone,
        role: user.role,
        isPhoneVerified: user.isPhoneVerified,
      },
    };
  }

  /**
   * Foydalanuvchiga yangi parol o'rnatish
   */
  async setPassword(userId: string, password: string): Promise<{ message: string }> {
    const passwordHash = await bcrypt.hash(password, 10);
    await this.userRepository.update(userId, { passwordHash });
    return { message: 'Parol muvaffaqiyatli o\'rnatildi' };
  }

  /**
   * Tokenlarni yangilash (Refresh token flow)
   */
  async refreshTokens(userId: string, refreshToken: string) {
    const user = await this.userRepository.findOne({
      where: { id: userId },
      select: ['id', 'phone', 'role', 'refreshToken'],
    });

    if (!user || !user.refreshToken) {
      throw new UnauthorizedException('Ruxsat etilmagan');
    }

    const isMatch = await bcrypt.compare(refreshToken, user.refreshToken);
    if (!isMatch) {
      throw new UnauthorizedException('Yaroqsiz refresh token');
    }

    const tokens = await this.generateTokens(user);
    const hashedRefreshToken = await bcrypt.hash(tokens.refreshToken, 10);
    await this.userRepository.update(user.id, {
      refreshToken: hashedRefreshToken,
    });

    return tokens;
  }

  /**
   * JWT validate callback
   */
  async validateUser(userId: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { id: userId } });
  }

  /**
   * Logout
   */
  async logout(userId: string): Promise<void> {
    await this.userRepository.update(userId, { refreshToken: undefined });
  }

  private async generateTokens(user: User): Promise<{ accessToken: string; refreshToken: string }> {
    const payload = {
      sub: user.id,
      phone: user.phone,
      role: user.role,
    };

    const [accessToken, refreshToken] = await Promise.all([
      this.jwtService.signAsync(payload, {
        secret: this.configService.get<string>('JWT_SECRET'),
        expiresIn: this.configService.get<string>('JWT_EXPIRATION', '15m'),
      }),
      this.jwtService.signAsync(payload, {
        secret: this.configService.get<string>('JWT_REFRESH_SECRET'),
        expiresIn: this.configService.get<string>('JWT_REFRESH_EXPIRATION', '7d'),
      }),
    ]);

    return { accessToken, refreshToken };
  }
}
