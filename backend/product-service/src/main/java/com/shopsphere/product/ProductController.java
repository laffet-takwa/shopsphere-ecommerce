package com.shopsphere.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;
    public ProductController(ProductRepository products) { this.products = products; }

    @GetMapping
    public Page<ProductResponse> browse(@RequestParam(required = false) String category,
                                        @RequestParam(required = false) String keyword,
                                        @PageableDefault(size = 24, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return products.browse(blankToNull(category), blankToNull(keyword), pageable).map(ProductResponse::from);
    }

    @GetMapping("/category/{category}")
    public Page<ProductResponse> byCategory(@PathVariable String category,
                                            @PageableDefault(size = 24, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return products.browse(category.trim(), null, pageable).map(ProductResponse::from);
    }

    @GetMapping("/search")
    public Page<ProductResponse> search(@RequestParam String keyword,
                                        @PageableDefault(size = 24, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return products.browse(null, blankToNull(keyword), pageable).map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id,
                               @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Role", required = false) String role) {
        Product product = find(id);
        if (!product.isActive() && !"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        return ProductResponse.from(product);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(products.save(new Product(request)));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = find(id);
        product.update(request);
        return ProductResponse.from(products.save(product));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { products.delete(find(id)); }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}