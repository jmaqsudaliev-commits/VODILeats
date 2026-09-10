import {
  Controller,
  Post,
  Body,
  UseGuards,
  Request,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { SendOtpDto, VerifyOtpDto, RefreshTokenDto, LoginPasswordDto, SetPasswordDto } from './dto/auth.dto';
import { JwtAuthGuard } from './guards/jwt-auth.guard';

@ApiTags('auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('send-otp')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'SMS OTP yuborish' })
  @ApiResponse({ status: 200, description: 'OTP muvaffaqiyatli yuborildi' })
  async sendOtp(@Body() dto: SendOtpDto) {
    return this.authService.sendOtp(dto.phone);
  }

  @Post('verify-otp')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'OTP tasdiqlash va ro\'yxatdan o\'tish (Ism va Parol bilan)' })
  @ApiResponse({ status: 200, description: 'Muvaffaqiyatli autentifikatsiya' })
  @ApiResponse({ status: 401, description: 'Noto\'g\'ri OTP kodi' })
  async verifyOtp(@Body() dto: VerifyOtpDto) {
    return this.authService.verifyOtp(dto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Telefon raqam va parol orqali kirish' })
  async login(@Body() dto: LoginPasswordDto) {
    return this.authService.loginWithPassword(dto);
  }

  @Post('login-password')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Telefon raqam va parol orqali kirish (boshqa telefondan yoki qayta kirish)' })
  @ApiResponse({ status: 200, description: 'Muvaffaqiyatli kirildi' })
  @ApiResponse({ status: 401, description: 'Telefon raqam yoki parol xato' })
  async loginPassword(@Body() dto: LoginPasswordDto) {
    return this.authService.loginWithPassword(dto);
  }

  @Post('set-password')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Parol o\'rnatish yoki yangilash' })
  async setPassword(@Request() req: any, @Body() dto: SetPasswordDto) {
    return this.authService.setPassword(req.user.id, dto.password);
  }

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Tokenlarni yangilash' })
  @ApiResponse({ status: 200, description: 'Yangi tokenlar qaytarildi' })
  async refreshTokens(@Request() req: any, @Body() dto: RefreshTokenDto) {
    return this.authService.refreshTokens(req.user.id, dto.refreshToken);
  }

  @Post('logout')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Tizimdan chiqish' })
  @ApiResponse({ status: 200, description: 'Muvaffaqiyatli chiqildi' })
  async logout(@Request() req: any) {
    await this.authService.logout(req.user.id);
    return { message: 'Tizimdan muvaffaqiyatli chiqildi' };
  }
}
