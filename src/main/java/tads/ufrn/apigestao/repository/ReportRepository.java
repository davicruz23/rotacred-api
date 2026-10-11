package tads.ufrn.apigestao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.Product;
import tads.ufrn.apigestao.enums.PaymentType;
import tads.ufrn.apigestao.enums.SaleStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT p.id, p.name, p.brand, COALESCE(p.amount, 0), p.value,
               CASE WHEN COALESCE(p.amount, 0) = 0 THEN 'OUT_OF_STOCK'
                    WHEN p.amount <= 10 THEN 'VERY_LOW'
                    WHEN p.amount <= 30 THEN 'LOW'
                    ELSE 'AVAILABLE' END
        FROM product p
        WHERE p.deleted_at IS NULL
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:stockStatus IS NULL
               OR (:stockStatus = 'OUT_OF_STOCK' AND COALESCE(p.amount, 0) = 0)
               OR (:stockStatus = 'VERY_LOW' AND p.amount BETWEEN 1 AND 10)
               OR (:stockStatus = 'LOW' AND p.amount BETWEEN 11 AND 30)
               OR (:stockStatus = 'AVAILABLE' AND p.amount > 30))
        ORDER BY CASE WHEN :sort = 'stockAsc' THEN p.amount END ASC,
                 CASE WHEN :sort = 'stockDesc' THEN p.amount END DESC,
                 CASE WHEN :sort = 'valueAsc' THEN p.value END ASC,
                 CASE WHEN :sort = 'valueDesc' THEN p.value END DESC,
                 p.name ASC
        """, countQuery = """
        SELECT COUNT(p.id) FROM product p
        WHERE p.deleted_at IS NULL
          AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:stockStatus IS NULL
               OR (:stockStatus = 'OUT_OF_STOCK' AND COALESCE(p.amount, 0) = 0)
               OR (:stockStatus = 'VERY_LOW' AND p.amount BETWEEN 1 AND 10)
               OR (:stockStatus = 'LOW' AND p.amount BETWEEN 11 AND 30)
               OR (:stockStatus = 'AVAILABLE' AND p.amount > 30))
        """, nativeQuery = true)
    Page<Object[]> inventory(@Param("name") String name, @Param("stockStatus") String stockStatus,
                             @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT p.id, p.name, p.brand, COALESCE(SUM(psi.quantity), 0),
               COALESCE(SUM(psi.quantity * p.value), 0)
        FROM pre_sale_item psi
        JOIN pre_sale ps ON ps.id = psi.pre_sale_id
        JOIN sale s ON s.pre_sale_id = ps.id
        JOIN product p ON p.id = psi.product_id
        WHERE (:startDate IS NULL OR s.sale_date >= :startDate)
          AND (:endDate IS NULL OR s.sale_date <= :endDate)
          AND (:city IS NULL OR LOWER((SELECT a.city FROM client c JOIN address a ON a.id = c.address_id WHERE c.id = ps.client_id)) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:sellerId IS NULL OR ps.seller_id = :sellerId)
          AND (:productId IS NULL OR p.id = :productId)
        GROUP BY p.id, p.name, p.brand
        ORDER BY CASE WHEN :sort = 'quantityAsc' THEN SUM(psi.quantity) END ASC,
                 CASE WHEN :sort <> 'quantityAsc' THEN SUM(psi.quantity) END DESC,
                 p.name ASC
        """, countQuery = """
        SELECT COUNT(*) FROM (
          SELECT p.id FROM pre_sale_item psi
          JOIN pre_sale ps ON ps.id = psi.pre_sale_id
          JOIN sale s ON s.pre_sale_id = ps.id
          JOIN product p ON p.id = psi.product_id
          JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id
          WHERE (:startDate IS NULL OR s.sale_date >= :startDate)
            AND (:endDate IS NULL OR s.sale_date <= :endDate)
            AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
            AND (:sellerId IS NULL OR ps.seller_id = :sellerId)
            AND (:productId IS NULL OR p.id = :productId)
          GROUP BY p.id
        ) x
        """, nativeQuery = true)
    Page<Object[]> productRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                  @Param("city") String city, @Param("sellerId") Long sellerId,
                                  @Param("productId") Long productId, @Param("sort") String sort,
                                  Pageable pageable);

    @Query(value = """
        SELECT c.id, c.date, c.description, u.id, u.name, COUNT(DISTINCT ci.product_id),
               COALESCE(SUM(ci.quantity), 0)
        FROM charging c
        LEFT JOIN charging_item ci ON ci.charging_id = c.id
        LEFT JOIN users u ON u.id = c.user_id
        WHERE c.deleted_at IS NULL
          AND (:startDate IS NULL OR c.date >= :startDate)
          AND (:endDate IS NULL OR c.date <= :endDate)
          AND (:responsibleId IS NULL OR c.user_id = :responsibleId)
        GROUP BY c.id, c.date, c.description, u.id, u.name
        ORDER BY CASE WHEN :sort = 'quantityAsc' THEN SUM(ci.quantity) END ASC,
                 CASE WHEN :sort <> 'quantityAsc' THEN c.date END DESC, c.id DESC
        """, countQuery = """
        SELECT COUNT(c.id) FROM charging c
        WHERE c.deleted_at IS NULL
          AND (:startDate IS NULL OR c.date >= :startDate)
          AND (:endDate IS NULL OR c.date <= :endDate)
          AND (:responsibleId IS NULL OR c.user_id = :responsibleId)
        """, nativeQuery = true)
    Page<Object[]> loadings(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                            @Param("responsibleId") Long responsibleId, @Param("sort") String sort,
                            Pageable pageable);

    @Query(value = """
        SELECT s.id, s.sale_date, c.id, c.name, a.city, se.id, su.name,
               co.id, cu.name, s.total, s.payment_method, s.status
        FROM sale s
        JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id
        JOIN address a ON a.id = c.address_id
        JOIN seller se ON se.id = ps.seller_id
        JOIN users su ON su.id = se.user_id
        LEFT JOIN collector co ON co.id = s.collector_id
        LEFT JOIN users cu ON cu.id = co.user_id
        WHERE (:startDate IS NULL OR s.sale_date >= :startDate)
          AND (:endDate IS NULL OR s.sale_date <= :endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:clientId IS NULL OR c.id = :clientId)
          AND (:sellerId IS NULL OR se.id = :sellerId)
          AND (:collectorId IS NULL OR co.id = :collectorId)
          AND (:productId IS NULL OR EXISTS (SELECT 1 FROM pre_sale_item x WHERE x.pre_sale_id = ps.id AND x.product_id = :productId))
          AND (:paymentMethod IS NULL OR s.payment_method = :paymentMethod)
          AND (:saleStatus IS NULL OR s.status = :saleStatus)
        ORDER BY CASE WHEN :sort = 'valueAsc' THEN s.total END ASC,
                 CASE WHEN :sort = 'valueDesc' THEN s.total END DESC,
                 CASE WHEN :sort = 'oldest' THEN s.sale_date END ASC,
                 s.sale_date DESC, s.id DESC
        """, countQuery = """
        SELECT COUNT(s.id)
        FROM sale s JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id
        JOIN seller se ON se.id = ps.seller_id LEFT JOIN collector co ON co.id = s.collector_id
        WHERE (:startDate IS NULL OR s.sale_date >= :startDate)
          AND (:endDate IS NULL OR s.sale_date <= :endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:clientId IS NULL OR c.id = :clientId) AND (:sellerId IS NULL OR se.id = :sellerId)
          AND (:collectorId IS NULL OR co.id = :collectorId)
          AND (:productId IS NULL OR EXISTS (SELECT 1 FROM pre_sale_item x WHERE x.pre_sale_id = ps.id AND x.product_id = :productId))
          AND (:paymentMethod IS NULL OR s.payment_method = :paymentMethod)
          AND (:saleStatus IS NULL OR s.status = :saleStatus)
        """, nativeQuery = true)
    Page<Object[]> sales(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                         @Param("city") String city, @Param("clientId") Long clientId,
                         @Param("sellerId") Long sellerId, @Param("collectorId") Long collectorId,
                         @Param("productId") Long productId, @Param("paymentMethod") String paymentMethod,
                         @Param("saleStatus") String saleStatus, @Param("sort") String sort, Pageable pageable);

    @Query("""
        SELECT COUNT(s), COALESCE(SUM(s.total), 0), COALESCE(AVG(s.total), 0)
        FROM Sale s JOIN s.preSale ps JOIN ps.client c JOIN c.address a JOIN ps.seller se
        WHERE (:startDate IS NULL OR s.saleDate >= :startDate) AND (:endDate IS NULL OR s.saleDate <= :endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:clientId IS NULL OR c.id = :clientId) AND (:sellerId IS NULL OR se.id = :sellerId)
          AND (:collectorId IS NULL OR s.collector.id = :collectorId) AND (:productId IS NULL OR EXISTS (SELECT x FROM PreSaleItem x WHERE x.preSale = ps AND x.product.id = :productId))
          AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod) AND (:saleStatus IS NULL OR s.status = :saleStatus)
        """)
    Object[] salesSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                          @Param("city") String city, @Param("clientId") Long clientId,
                          @Param("sellerId") Long sellerId, @Param("collectorId") Long collectorId,
                          @Param("productId") Long productId, @Param("paymentMethod") PaymentType paymentMethod,
                          @Param("saleStatus") SaleStatus saleStatus);

    @Query(value = """
        SELECT se.id, su.name, COUNT(DISTINCT s.id), COALESCE(SUM(s.total), 0)
        FROM sale s JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN seller se ON se.id=ps.seller_id JOIN users su ON su.id=se.user_id
        JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id
        WHERE (:startDate IS NULL OR s.sale_date>=:startDate) AND (:endDate IS NULL OR s.sale_date<=:endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%',:city,'%')))
        GROUP BY se.id, su.name ORDER BY CASE WHEN :sort='valueAsc' THEN SUM(s.total) END ASC, COUNT(DISTINCT s.id) DESC
        """, countQuery = "SELECT COUNT(*) FROM (SELECT se.id FROM sale s JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN seller se ON se.id=ps.seller_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id WHERE (:startDate IS NULL OR s.sale_date>=:startDate) AND (:endDate IS NULL OR s.sale_date<=:endDate) AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%',:city,'%'))) GROUP BY se.id) x", nativeQuery = true)
    Page<Object[]> sellerRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                 @Param("city") String city, @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT s.payment_method, COUNT(s.id), COALESCE(SUM(s.total), 0)
        FROM sale s JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id
        WHERE (:startDate IS NULL OR s.sale_date>=:startDate) AND (:endDate IS NULL OR s.sale_date<=:endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%',:city,'%')))
        GROUP BY s.payment_method ORDER BY CASE WHEN :sort='valueAsc' THEN SUM(s.total) END ASC, SUM(s.total) DESC
        """, countQuery = "SELECT COUNT(DISTINCT s.payment_method) FROM sale s JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id WHERE (:startDate IS NULL OR s.sale_date>=:startDate) AND (:endDate IS NULL OR s.sale_date<=:endDate) AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%',:city,'%')))" , nativeQuery = true)
    Page<Object[]> paymentRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                  @Param("city") String city, @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT i.id, s.id, c.id, c.name, a.city, co.id, cu.name, i.due_date, i.amount,
               i.paid, i.paid_amount, i.payment_date, i.payment_type,
               CASE WHEN i.paid = 0 AND i.due_date < CURDATE() THEN DATEDIFF(CURDATE(), i.due_date) ELSE 0 END
        FROM installment i JOIN sale s ON s.id = i.sale_id JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id
        LEFT JOIN collector co ON co.id = s.collector_id LEFT JOIN users cu ON cu.id = co.user_id
        WHERE (:dateType = 'PAYMENT' AND (:startDate IS NULL OR i.payment_date >= :startDate) AND (:endDate IS NULL OR i.payment_date < DATE_ADD(:endDate, INTERVAL 1 DAY))
               OR :dateType <> 'PAYMENT' AND (:startDate IS NULL OR i.due_date >= :startDate) AND (:endDate IS NULL OR i.due_date <= :endDate))
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:clientId IS NULL OR c.id = :clientId) AND (:collectorId IS NULL OR co.id = :collectorId)
          AND (:paymentType IS NULL OR i.payment_type = :paymentType)
          AND (:status IS NULL OR (:status = 'PAID' AND i.paid = 1) OR (:status = 'PENDING' AND i.paid = 0)
               OR (:status = 'OVERDUE' AND i.paid = 0 AND i.due_date < CURDATE())
               OR (:status = 'UPCOMING' AND i.paid = 0 AND i.due_date >= CURDATE()))
        ORDER BY CASE WHEN :sort = 'valueAsc' THEN i.amount END ASC,
                 CASE WHEN :sort = 'valueDesc' THEN i.amount END DESC,
                 CASE WHEN :sort = 'oldest' THEN i.due_date END ASC, i.due_date DESC, i.id DESC
        """, countQuery = """
        SELECT COUNT(i.id) FROM installment i JOIN sale s ON s.id = i.sale_id JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id LEFT JOIN collector co ON co.id = s.collector_id
        WHERE (:dateType = 'PAYMENT' AND (:startDate IS NULL OR i.payment_date >= :startDate) AND (:endDate IS NULL OR i.payment_date < DATE_ADD(:endDate, INTERVAL 1 DAY)) OR :dateType <> 'PAYMENT' AND (:startDate IS NULL OR i.due_date >= :startDate) AND (:endDate IS NULL OR i.due_date <= :endDate))
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND (:clientId IS NULL OR c.id = :clientId)
          AND (:collectorId IS NULL OR co.id = :collectorId) AND (:paymentType IS NULL OR i.payment_type = :paymentType)
          AND (:status IS NULL OR (:status = 'PAID' AND i.paid = 1) OR (:status = 'PENDING' AND i.paid = 0) OR (:status = 'OVERDUE' AND i.paid = 0 AND i.due_date < CURDATE()) OR (:status = 'UPCOMING' AND i.paid = 0 AND i.due_date >= CURDATE()))
        """, nativeQuery = true)
    Page<Object[]> collections(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                               @Param("dateType") String dateType, @Param("city") String city,
                               @Param("clientId") Long clientId, @Param("collectorId") Long collectorId,
                               @Param("paymentType") String paymentType, @Param("status") String status,
                               @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT COALESCE(SUM(CASE WHEN i.paid = 1 THEN COALESCE(i.paid_amount, i.amount) ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN i.paid = 0 THEN i.amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN i.paid = 0 AND i.due_date < CURDATE() THEN i.amount ELSE 0 END), 0),
               SUM(CASE WHEN i.paid = 1 THEN 1 ELSE 0 END), SUM(CASE WHEN i.paid = 0 THEN 1 ELSE 0 END),
               SUM(CASE WHEN i.paid = 0 AND i.due_date < CURDATE() THEN 1 ELSE 0 END)
        FROM installment i JOIN sale s ON s.id = i.sale_id JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id LEFT JOIN collector co ON co.id = s.collector_id
        WHERE (:startDate IS NULL OR (:dateType = 'PAYMENT' AND i.payment_date >= :startDate OR :dateType <> 'PAYMENT' AND i.due_date >= :startDate))
          AND (:endDate IS NULL OR (:dateType = 'PAYMENT' AND i.payment_date < DATE_ADD(:endDate, INTERVAL 1 DAY) OR :dateType <> 'PAYMENT' AND i.due_date <= :endDate))
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND (:clientId IS NULL OR c.id = :clientId)
          AND (:collectorId IS NULL OR co.id = :collectorId) AND (:paymentType IS NULL OR i.payment_type = :paymentType)
        """, nativeQuery = true)
    Object[] collectionsSummary(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                @Param("dateType") String dateType, @Param("city") String city,
                                @Param("clientId") Long clientId, @Param("collectorId") Long collectorId,
                                @Param("paymentType") String paymentType);

    @Query(value = """
        SELECT c.id, c.name, a.city, MIN(s.sale_date), MAX(s.sale_date), COUNT(DISTINCT s.id),
               COALESCE(SUM(s.total), 0),
               COALESCE((SELECT SUM(CASE WHEN i2.paid = 1 THEN COALESCE(i2.paid_amount, i2.amount) ELSE 0 END)
                         FROM installment i2 JOIN sale s2 ON s2.id = i2.sale_id JOIN pre_sale ps2 ON ps2.id = s2.pre_sale_id
                         WHERE ps2.client_id = c.id AND (:startDate IS NULL OR s2.sale_date >= :startDate) AND (:endDate IS NULL OR s2.sale_date <= :endDate)), 0),
               COALESCE((SELECT COUNT(i3.id)
                         FROM installment i3 JOIN sale s3 ON s3.id = i3.sale_id JOIN pre_sale ps3 ON ps3.id = s3.pre_sale_id
                         WHERE ps3.client_id = c.id AND i3.paid = 0 AND i3.due_date < CURDATE()
                           AND (:startDate IS NULL OR s3.sale_date >= :startDate) AND (:endDate IS NULL OR s3.sale_date <= :endDate)), 0)
        FROM client c JOIN address a ON a.id = c.address_id JOIN pre_sale ps ON ps.client_id = c.id
        JOIN sale s ON s.pre_sale_id = ps.id
        WHERE (:startDate IS NULL OR s.sale_date >= :startDate) AND (:endDate IS NULL OR s.sale_date <= :endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
        GROUP BY c.id, c.name, a.city
        ORDER BY CASE WHEN :sort = 'valueAsc' THEN SUM(s.total) END ASC,
                 CASE WHEN :sort = 'oldest' THEN MIN(s.sale_date) END ASC,
                 CASE WHEN :sort = 'paidDesc' THEN COALESCE((SELECT SUM(CASE WHEN i2.paid = 1 THEN COALESCE(i2.paid_amount, i2.amount) ELSE 0 END) FROM installment i2 JOIN sale s2 ON s2.id=i2.sale_id JOIN pre_sale ps2 ON ps2.id=s2.pre_sale_id WHERE ps2.client_id=c.id), 0) END DESC,
                 CASE WHEN :sort = 'salesDesc' THEN COUNT(DISTINCT s.id) END DESC,
                 CASE WHEN :sort = 'overdueDesc' THEN COALESCE((SELECT COUNT(i3.id) FROM installment i3 JOIN sale s3 ON s3.id=i3.sale_id JOIN pre_sale ps3 ON ps3.id=s3.pre_sale_id WHERE ps3.client_id=c.id AND i3.paid=0 AND i3.due_date<CURDATE()), 0) END DESC,
                 SUM(s.total) DESC
        """, countQuery = """
        SELECT COUNT(*) FROM (SELECT c.id FROM client c JOIN address a ON a.id = c.address_id JOIN pre_sale ps ON ps.client_id = c.id JOIN sale s ON s.pre_sale_id = ps.id WHERE (:startDate IS NULL OR s.sale_date >= :startDate) AND (:endDate IS NULL OR s.sale_date <= :endDate) AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) GROUP BY c.id) x
        """, nativeQuery = true)
    Page<Object[]> clientRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                 @Param("city") String city, @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT a.city, COUNT(DISTINCT s.id), COALESCE(SUM(s.total), 0), COUNT(DISTINCT c.id),
               COALESCE((SELECT SUM(oi.amount) FROM installment oi JOIN sale os ON os.id = oi.sale_id JOIN pre_sale ops ON ops.id = os.pre_sale_id JOIN client oc ON oc.id = ops.client_id JOIN address oa ON oa.id = oc.address_id WHERE oa.city = a.city AND oi.paid = 0 AND oi.due_date < CURDATE()), 0)
        FROM sale s JOIN pre_sale ps ON ps.id = s.pre_sale_id JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id
        WHERE (:startDate IS NULL OR s.sale_date >= :startDate) AND (:endDate IS NULL OR s.sale_date <= :endDate)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
        GROUP BY a.city
        ORDER BY CASE WHEN :sort = 'revenueAsc' THEN SUM(s.total) END ASC,
                 CASE WHEN :sort = 'salesAsc' THEN COUNT(DISTINCT s.id) END ASC, SUM(s.total) DESC
        """, nativeQuery = true)
    Page<Object[]> cityRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                               @Param("city") String city, @Param("sort") String sort, Pageable pageable);

    @Query(value = """
        SELECT p.id, p.name, COALESCE(SUM(psi.quantity), 0), COALESCE(SUM(psi.quantity * p.value), 0)
        FROM pre_sale_item psi JOIN pre_sale ps ON ps.id = psi.pre_sale_id JOIN sale s ON s.pre_sale_id = ps.id
        JOIN product p ON p.id = psi.product_id JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id
        WHERE LOWER(a.city) = LOWER(:city) AND (:startDate IS NULL OR s.sale_date >= :startDate) AND (:endDate IS NULL OR s.sale_date <= :endDate)
        GROUP BY p.id, p.name ORDER BY SUM(psi.quantity) DESC, p.name ASC
        """, countQuery = "SELECT COUNT(*) FROM (SELECT p.id FROM pre_sale_item psi JOIN pre_sale ps ON ps.id=psi.pre_sale_id JOIN sale s ON s.pre_sale_id=ps.id JOIN product p ON p.id=psi.product_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id WHERE LOWER(a.city)=LOWER(:city) AND (:startDate IS NULL OR s.sale_date>=:startDate) AND (:endDate IS NULL OR s.sale_date<=:endDate) GROUP BY p.id) x", nativeQuery = true)
    Page<Object[]> cityProductRanking(@Param("city") String city, @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query(value = """
        SELECT sr.id, sr.sale_id, sr.return_date, sr.product_id, p.name, sr.quantity_returned,
               sr.sale_status, sr.description, c.id, c.name, a.city, se.id, su.name, co.id, cu.name
        FROM sale_return sr JOIN sale s ON s.id = sr.sale_id JOIN pre_sale ps ON ps.id = s.pre_sale_id
        JOIN client c ON c.id = ps.client_id JOIN address a ON a.id = c.address_id JOIN seller se ON se.id = ps.seller_id
        JOIN users su ON su.id = se.user_id LEFT JOIN collector co ON co.id = s.collector_id LEFT JOIN users cu ON cu.id = co.user_id
        LEFT JOIN product p ON p.id = sr.product_id
        WHERE (:startDate IS NULL OR sr.return_date >= :startDate) AND (:endDate IS NULL OR sr.return_date < DATE_ADD(:endDate, INTERVAL 1 DAY))
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND (:clientId IS NULL OR c.id = :clientId)
          AND (:sellerId IS NULL OR se.id = :sellerId) AND (:collectorId IS NULL OR co.id = :collectorId)
          AND (:productId IS NULL OR sr.product_id = :productId) AND (:status IS NULL OR sr.sale_status = :status)
        ORDER BY sr.return_date DESC, sr.id DESC
        """, countQuery = """
        SELECT COUNT(sr.id) FROM sale_return sr JOIN sale s ON s.id=sr.sale_id JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id JOIN seller se ON se.id=ps.seller_id LEFT JOIN collector co ON co.id=s.collector_id WHERE (:startDate IS NULL OR sr.return_date >= :startDate) AND (:endDate IS NULL OR sr.return_date < DATE_ADD(:endDate, INTERVAL 1 DAY)) AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND (:clientId IS NULL OR c.id=:clientId) AND (:sellerId IS NULL OR se.id=:sellerId) AND (:collectorId IS NULL OR co.id=:collectorId) AND (:productId IS NULL OR sr.product_id=:productId) AND (:status IS NULL OR sr.sale_status=:status)
        """, nativeQuery = true)
    Page<Object[]> returns(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                           @Param("city") String city, @Param("clientId") Long clientId,
                           @Param("sellerId") Long sellerId, @Param("collectorId") Long collectorId,
                           @Param("productId") Long productId, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(sr), COALESCE(SUM(sr.quantityReturned), 0) FROM SaleReturn sr WHERE (:startDate IS NULL OR sr.returnDate >= :startDate) AND (:endDate IS NULL OR sr.returnDate < :endDate) AND (:status IS NULL OR sr.saleStatus = :status)")
    Object[] returnsSummary(@Param("startDate") java.time.OffsetDateTime startDate,
                            @Param("endDate") java.time.OffsetDateTime endDate,
                            @Param("status") tads.ufrn.apigestao.enums.SaleStatus status);

    @Query(value = """
        SELECT p.id, p.name, COALESCE(SUM(sr.quantity_returned), 0)
        FROM sale_return sr LEFT JOIN product p ON p.id = sr.product_id
        WHERE (:startDate IS NULL OR sr.return_date >= :startDate) AND (:endDate IS NULL OR sr.return_date < DATE_ADD(:endDate, INTERVAL 1 DAY))
          AND (:status IS NULL OR sr.sale_status = :status)
        GROUP BY p.id, p.name ORDER BY SUM(sr.quantity_returned) DESC, p.name ASC
        """, countQuery = "SELECT COUNT(*) FROM (SELECT sr.product_id FROM sale_return sr WHERE (:startDate IS NULL OR sr.return_date >= :startDate) AND (:endDate IS NULL OR sr.return_date < DATE_ADD(:endDate, INTERVAL 1 DAY)) AND (:status IS NULL OR sr.sale_status = :status) GROUP BY sr.product_id) x", nativeQuery = true)
    Page<Object[]> returnProductRanking(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                        @Param("status") String status, Pageable pageable);

    @Query(value = """
        SELECT sr.id, sr.sale_id, sr.return_date, sr.product_id, p.name, sr.quantity_returned, sr.sale_status, sr.description,
               c.id, c.name, a.city, se.id, su.name, co.id, cu.name
        FROM sale_return sr JOIN sale s ON s.id=sr.sale_id JOIN pre_sale ps ON ps.id=s.pre_sale_id JOIN client c ON c.id=ps.client_id JOIN address a ON a.id=c.address_id JOIN seller se ON se.id=ps.seller_id JOIN users su ON su.id=se.user_id LEFT JOIN collector co ON co.id=s.collector_id LEFT JOIN users cu ON cu.id=co.user_id LEFT JOIN product p ON p.id=sr.product_id
        WHERE sr.sale_status = 'DEFEITO_PRODUTO' AND (:startDate IS NULL OR sr.return_date >= :startDate) AND (:endDate IS NULL OR sr.return_date < DATE_ADD(:endDate, INTERVAL 1 DAY))
        ORDER BY sr.return_date DESC, sr.id DESC
        """, countQuery = "SELECT COUNT(sr.id) FROM sale_return sr WHERE sr.sale_status='DEFEITO_PRODUTO' AND (:startDate IS NULL OR sr.return_date>=:startDate) AND (:endDate IS NULL OR sr.return_date<DATE_ADD(:endDate, INTERVAL 1 DAY))", nativeQuery = true)
    Page<Object[]> guaranteeRequests(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
}
