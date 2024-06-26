package org.likelion.productproject.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelion.productproject.domain.Product;
import org.likelion.productproject.domain.ProductRepository;
import org.likelion.productproject.dto.ProductResponseDto;
import org.likelion.productproject.dto.ProductSaveRequestDto;
import org.likelion.productproject.dto.ProductUpdateRequestDto;
import org.likelion.productproject.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.likelion.productproject.fixture.ProductFixture.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductService productService;

    @Autowired
    ProductRepository productRepository;

    @AfterEach
    public void afterEach() {
        productRepository.clear();
    }

    @Test
    @DisplayName("상품을 저장한다")
    public void saveProduct() throws Exception {

        final ProductSaveRequestDto requestDto = ProductSaveRequestDto.builder()
                .productId(PRODUCT_1.getProductId())
                .name(PRODUCT_1.getName())
                .price(PRODUCT_1.getPrice())
                .build();

        String url = "http://localhost:" + port + "/products";

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .content(asJsonString(requestDto))
                        .contentType("application/json"))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        List<Product> all = productRepository.findAll();
        assertThat(all.get(0).getProductId()).isEqualTo(PRODUCT_1.getProductId());
        assertThat(all.get(0).getName()).isEqualTo(PRODUCT_1.getName());
        assertThat(all.get(0).getPrice()).isEqualTo(PRODUCT_1.getPrice());
    }

    @Test
    @DisplayName("상품을 조회한다")
    public void getProduct() throws Exception {
        productRepository.save(PRODUCT_2);

        String url = "http://localhost:" + port + "/products/" + PRODUCT_2.getId();

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(url)).andReturn();

        final ProductResponseDto productResponse = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        assertThat(productResponse.getProductId()).isEqualTo(PRODUCT_2.getProductId());
        assertThat(productResponse.getName()).isEqualTo(PRODUCT_2.getName());
        assertThat(productResponse.getPrice()).isEqualTo(PRODUCT_2.getPrice());
    }

    @Test
    @DisplayName("상품 전체를 조회한다")
    public void getAllProduct() throws Exception {
        // given
        productRepository.save(PRODUCT_1);
        productRepository.save(PRODUCT_2);
        productRepository.save(PRODUCT_3);

        String url = "http://localhost:" + port + "/products";

        // when
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(url)).andReturn();

        // then
        final List<ProductResponseDto> actualResponses = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        List<ProductResponseDto> expectedResponses = productService.findAllProduct();
        assertThat(actualResponses).usingRecursiveComparison().isEqualTo(expectedResponses);
    }

    @Test
    @DisplayName("상품을 수정한다")
    public void updateProduct() throws Exception {

        final Product savedProduct = productRepository.save(PRODUCT_1);

        Long updateId = savedProduct.getId();
        String newName = "아이스티";
        String newPrice = "3000";

        ProductUpdateRequestDto requestDto = ProductUpdateRequestDto.builder()
                .name(newName)
                .price(newPrice)
                .build();

        String url = "http://localhost:" + port + "/products/" + updateId;

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.patch(url)
                        .content(asJsonString(requestDto))
                        .contentType("application/json"))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        List<Product> all = productRepository.findAll();
        assertThat(all.get(0).getName()).isEqualTo(newName);
        assertThat(all.get(0).getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("상품을 삭제한다")
    public void deleteProduct() throws Exception {

        final Product savedProduct = productRepository.save(PRODUCT_1);

        String url = "http://localhost:" + port + "/products/" + savedProduct.getId();

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.delete(url)).andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());

        List<Product> all = productRepository.findAll();
        assertThat(all.size()).isEqualTo(0);
    }

    private String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
