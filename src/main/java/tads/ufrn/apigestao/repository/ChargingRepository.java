package tads.ufrn.apigestao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.Charging;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingRepository extends JpaRepository<Charging, Long> {

    List<Charging> findAllByUserIdAndDeletedAtIsNull(Long userId);

    List<Charging> findAllByDeletedAtIsNull();

    @Query("""
    SELECT COUNT(*)
    FROM Charging c
""")
    Long countDistinctCities();

    @Query("""
        SELECT c FROM Charging c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.product p
        WHERE c.id = :id
    """)
    Optional<Charging> findWithItems(Long id);

    @Query("""
    SELECT DISTINCT c
    FROM Charging c
    LEFT JOIN FETCH c.items i
    WHERE (:name IS NULL OR i.product.name LIKE %:name%)
""")
    Page<Charging> findAllWithItems(@Param("name") String nameProduct, Pageable pageable);

    @Query("""
        SELECT DISTINCT c
        FROM Charging c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.product p
        WHERE c.id IN :ids
    """)
    List<Charging> findAllByIdInWithItems(@Param("ids") List<Long> ids);

    @Query(value = """
        SELECT DISTINCT c.id
        FROM Charging c
        LEFT JOIN c.items i
        LEFT JOIN i.product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        """,
            countQuery = """
        SELECT COUNT(DISTINCT c.id)
        FROM Charging c
        LEFT JOIN c.items i
        LEFT JOIN i.product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        """)
    Page<Long> findIdsByFilter(@Param("name") String name, Pageable pageable);

    @Query("""
        SELECT DISTINCT c
        FROM Charging c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.product p
        WHERE c.id IN :ids
        AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        """)
    List<Charging> findAllByIdInWithItems(@Param("ids") List<Long> ids, @Param("name") String name);


    @Query("""
    SELECT DISTINCT c
    FROM Charging c
    LEFT JOIN FETCH c.items i
    LEFT JOIN i.product p
    WHERE c.deletedAt IS NULL
    AND (
        :search IS NULL
        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))
    )
""")
    List<Charging> findAllCurrentWithItems(@Param("search") String search);

    @Query("""
    SELECT DISTINCT c
    FROM Charging c
    LEFT JOIN FETCH c.items
    WHERE c.id = :id
""")
    Optional<Charging> findByIdWithItems(Long id);

    Optional<Charging> findFirstByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Charging> findFirstBy();

}
