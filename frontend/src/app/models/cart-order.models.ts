// Các kiểu dữ liệu khớp với JSON mà backend (CartDTOs, OrderDTOs) trả về.

export interface CartItem {
  productId: number;
  title: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  availableStock: number;
  insufficient: boolean;
  missingQuantity: number;
  lineTotal: number;
}

export interface Cart {
  items: CartItem[];
  totalQuantity: number;
  totalExcludingVat: number;
  hasInsufficientStock: boolean;
}

export interface DevProduct {
  id: number;
  title: string;
  price: number;
  stock: number;
}

export interface DeliveryInfo {
  customerName: string;
  phone: string;
  email: string;
  address: string;
  province: string;
}

export interface ShippingFee {
  shippingFee: number;
  freeShippingApplied: boolean;
}

export interface StockShortage {
  productId: number;
  title: string;
  requested: number;
  available: number;
}

export interface InvoiceLine {
  productId: number;
  title: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Invoice {
  pendingOrderId: string;
  items: InvoiceLine[];
  totalExcludingVat: number;
  vatAmount: number;
  totalIncludingVat: number;
  shippingFee: number;
  totalAmount: number;
  delivery: DeliveryInfo;
}

export interface OrderResult {
  orderId: number;
  status: string;
  customerName: string;
  phone: string;
  email: string;
  address: string;
  province: string;
  totalAmount: number;
  items: InvoiceLine[];
  createdAt: string;
  transactionId: string | null;
  transactionContent: string | null;
  transactionTime: string | null;
}

/** Thân lỗi backend trả về: { code, message, details } */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: any;
}

export const PROVINCES: string[] = [
  'Hà Nội', 'TP. Hồ Chí Minh', 'Hải Phòng', 'Đà Nẵng', 'Cần Thơ', 'An Giang', 'Bà Rịa - Vũng Tàu',
  'Bắc Giang', 'Bắc Kạn', 'Bạc Liêu', 'Bắc Ninh', 'Bến Tre', 'Bình Định', 'Bình Dương', 'Bình Phước',
  'Bình Thuận', 'Cà Mau', 'Cao Bằng', 'Đắk Lắk', 'Đắk Nông', 'Điện Biên', 'Đồng Nai', 'Đồng Tháp',
  'Gia Lai', 'Hà Giang', 'Hà Nam', 'Hà Tĩnh', 'Hải Dương', 'Hậu Giang', 'Hòa Bình', 'Hưng Yên',
  'Khánh Hòa', 'Kiên Giang', 'Kon Tum', 'Lai Châu', 'Lâm Đồng', 'Lạng Sơn', 'Lào Cai', 'Long An',
  'Nam Định', 'Nghệ An', 'Ninh Bình', 'Ninh Thuận', 'Phú Thọ', 'Phú Yên', 'Quảng Bình', 'Quảng Nam',
  'Quảng Ngãi', 'Quảng Ninh', 'Quảng Trị', 'Sóc Trăng', 'Sơn La', 'Tây Ninh', 'Thái Bình',
  'Thái Nguyên', 'Thanh Hóa', 'Thừa Thiên Huế', 'Tiền Giang', 'Trà Vinh', 'Tuyên Quang', 'Vĩnh Long',
  'Vĩnh Phúc', 'Yên Bái',
];
