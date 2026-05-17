package br.com.data.salesprocessor.repository;

import br.com.data.salesprocessor.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, String> {

}
