package com.auction.core.users;

import com.auction.core.Entity;
import com.auction.core.exception.auction.InvalidBidException;
import com.auction.core.exception.wallet.InsufficientBalanceException;
import com.auction.core.exception.wallet.InvalidAmountException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Thực thể User dưới dạng Rich Domain Model (Reflection-free).
 * Đóng gói tuyệt đối trạng thái tài chính phạm vi private để bảo toàn tính toàn vẹn.
 * Không implements Serializable để thiết lập ranh giới an toàn tại Server Node.
 */
public abstract sealed class User extends Entity permits StandardUser, Admin {

    public enum Role {
        STANDARD,
        ADMIN
    }

    // ID có thể gán một lần duy nhất (Reflection-free Write-Once)
    protected Integer id;
    // username là final chỉ về mặt danh tính khởi tạo, nhưng hỗ trợ cập nhật qua setter có kiểm soát
    protected String username;
    protected final Role role;

    protected String password;
    protected String fullName;
    protected String email;
    protected Boolean isActive;

    // Đóng gói mạnh: private tuyệt đối để ngăn lớp con can thiệp trực tiếp
    private BigDecimal balance;
    private BigDecimal lockedBalance;

    /**
     * Constructor phạm vi package-private. Chỉ dành cho UserFactory (cùng package) gọi.
     */
    User(
            Integer id,
            String username,
            String password,
            String fullName,
            String email,
            BigDecimal balance,
            BigDecimal lockedBalance,
            Role role,
            Boolean isActive) {
        super();
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username không được null");
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.lockedBalance = lockedBalance != null ? lockedBalance : BigDecimal.ZERO;
        this.role = Objects.requireNonNull(role, "Role không được null");
        this.isActive = isActive != null ? isActive : true;
    }

    // --- Tầng Phân Quyền Đa Hình Mặc định (Dynamic Dispatch) ---

    /** Kiểm tra quyền tham gia đặt giá thầu của tài khoản. */
    public boolean canBid() {
        return false;
    }

    /** Kiểm tra quyền đăng bán sản phẩm của tài khoản. */
    public boolean canSell() {
        return false;
    }

    /** Kiểm tra quyền truy cập vào trang quản trị hệ thống. */
    public boolean canManageSystem() {
        return false;
    }

    // --- Các phương thức Hành vi Nghiệp vụ Tài chính Nguyên tử ---

    /**
     * Đóng băng một khoản tiền đặt cọc khi đặt giá thầu (Bid).
     *
     * @param amount Số tiền cần đóng băng (phải lớn hơn 0)
     * @throws InvalidBidException         Nếu số tiền nhỏ hơn hoặc bằng 0
     * @throws InsufficientBalanceException Nếu số dư khả dụng không đủ
     */
    public void holdBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Số tiền đóng băng phải lớn hơn 0");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Số dư khả dụng không đủ để thực hiện đặt giá thầu");
        }
        this.balance = this.balance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
        updateTimestamp();
    }

    /**
     * Khấu trừ hoàn toàn số tiền đang bị đóng băng khi đấu giá thành công.
     *
     * @param amount Số tiền khấu trừ (phải lớn hơn 0)
     * @throws InvalidBidException  Nếu số tiền nhỏ hơn hoặc bằng 0
     * @throws IllegalStateException Nếu số tiền đóng băng thực tế không đủ
     */
    public void commitBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Số tiền khấu trừ phải lớn hơn 0");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Số tiền đóng băng thực tế nhỏ hơn số tiền cần khấu trừ");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        updateTimestamp();
    }

    /**
     * Hoàn trả lại số tiền đặt cọc khi có người khác trả giá cao hơn (Outbid).
     *
     * @param amount Số tiền hoàn trả (phải lớn hơn 0)
     * @throws InvalidBidException  Nếu số tiền nhỏ hơn hoặc bằng 0
     * @throws IllegalStateException Nếu số tiền đóng băng thực tế không đủ
     */
    public void refundBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Số tiền hoàn trả phải lớn hơn 0");
        }
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Số tiền đóng băng không đủ để thực hiện hoàn tiền");
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
        this.balance = this.balance.add(amount);
        updateTimestamp();
    }

    /**
     * Nạp tiền vào ví người dùng.
     *
     * @param amount Số tiền nạp vào (phải lớn hơn 0)
     * @throws InvalidAmountException Nếu số tiền là null, bằng 0 hoặc âm
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Số tiền nạp phải lớn hơn 0");
        }
        this.balance = this.balance.add(amount);
        updateTimestamp();
    }

    /**
     * Rút tiền từ ví người dùng.
     *
     * @param amount Số tiền rút ra (phải lớn hơn 0)
     * @throws InvalidAmountException       Nếu số tiền là null, bằng 0 hoặc âm
     * @throws InsufficientBalanceException Nếu số dư khả dụng không đủ
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Số tiền rút phải lớn hơn 0");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Số dư khả dụng không đủ để thực hiện rút tiền");
        }
        this.balance = this.balance.subtract(amount);
        updateTimestamp();
    }

    // --- Getters cho thông tin Domain ---

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getLockedBalance() {
        return lockedBalance;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Cơ chế Write-Once Identity: Triệt tiêu hoàn toàn Deep Reflection.
     * Chỉ được gọi từ UserDao sau khi INSERT thành công và nhận về Generated Key.
     *
     * @param id Mã định danh duy nhất từ hệ thống lưu trữ
     * @throws IllegalStateException Nếu thực thể đã có ID từ trước
     */
    public void setId(Integer id) {
        if (this.id != null) {
            throw new IllegalStateException("ID của thực thể đã tồn tại và không thể sửa đổi!");
        }
        this.id = Objects.requireNonNull(id, "ID gán vào thực thể không được null");
        updateTimestamp();
    }

    public void setUsername(String username) {
        this.username = username;
        updateTimestamp();
    }

    public void setPassword(String password) {
        this.password = password;
        updateTimestamp();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        updateTimestamp();
    }

    public void setEmail(String email) {
        this.email = email;
        updateTimestamp();
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        updateTimestamp();
    }
}
