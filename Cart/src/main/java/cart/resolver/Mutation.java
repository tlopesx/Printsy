package cart.resolver;

import cart.service.CartService;
import cart.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class Mutation {

    private final CartService cartService;

    @Autowired
    public Mutation(CartService cartService) {
        this.cartService = cartService;
    }

    @MutationMapping
    public String deleteCartItemsByUserId(@Argument Long userId) {
        cartService.deleteCartItemsByUserId(userId);
        return "Cart items deleted successfully for user ID: " + userId;
    }

    @MutationMapping
    public String addItemtoCart(@Argument String imageId, @Argument Long stockId, @Argument Integer price, @Argument Long userId) {
        return cartService.addItemToCart(imageId, stockId, price, userId);
    }

    @MutationMapping
    public boolean completePurchase(@Argument Long userId) throws JsonProcessingException {
        return cartService.completePurchase(userId);
    }

}
