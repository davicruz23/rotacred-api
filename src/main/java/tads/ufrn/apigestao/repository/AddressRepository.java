package tads.ufrn.apigestao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tads.ufrn.apigestao.domain.Address;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("""
    SELECT COUNT(DISTINCT (TRIM(UPPER(a.city))))
    FROM Address a
""")
    Long countDistinctCities();

    @Query("SELECT DISTINCT a.city FROM Address a ORDER BY a.city ASC")
    List<String> findAllDistinctCities();

    @Query("SELECT DISTINCT TRIM(a.city) FROM Address a "
            + "WHERE a.city IS NOT NULL AND TRIM(a.city) <> '' "
            + "ORDER BY TRIM(a.city) ASC")
    List<String> findAllDistinctCityNames();
}
