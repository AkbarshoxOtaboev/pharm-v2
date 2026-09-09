package uz.uwon.pharm.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.uwon.pharm.utils.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findUsersByRoleAndStatus(Role role, Status status);
    boolean existsByUsername(String username);

    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE role = 'ADMIN') AS adminCount,
                COUNT(*) FILTER (WHERE role = 'OPERATOR') AS operatorCount,
                COUNT(*) FILTER (WHERE role = 'COURIER') AS courierCount
            FROM _users
            """, nativeQuery = true)
    UserStatResponse getUserStatistics();
}
