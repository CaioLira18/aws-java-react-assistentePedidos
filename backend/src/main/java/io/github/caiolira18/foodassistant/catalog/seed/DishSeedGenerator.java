package io.github.caiolira18.foodassistant.catalog.seed;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DishSeedGenerator {

    private DishSeedGenerator() {
    }

    private record Base(String name, String description, int priceCents, int prepMinutes, List<String> tags) {
    }

    private static Base base(String name, String description, int priceCents, int prepMinutes, String... tags) {
        return new Base(name, description, priceCents, prepMinutes, List.of(tags));
    }

    private static final List<Base> BASES = List.of(
            base("Strogonoff de Cogumelos", "Cogumelos salteados em molho cremoso de tomate e creme de leite, com arroz branco e batata palha.", 3290, 35, "vegetariano"),
            base("Moqueca de Palmito", "Palmito pupunha cozido em leite de coco, dendê e pimentões, com arroz e farofa de azeite.", 3490, 40, "vegano", "sem-gluten"),
            base("Feijoada Vegana", "Feijão preto com legumes defumados e cogumelos, arroz, couve refogada e laranja.", 3190, 45, "vegano", "sem-gluten"),
            base("Risoto de Limão Siciliano", "Arroz arbóreo cremoso em caldo de legumes, com limão siciliano e parmesão.", 3890, 35, "vegetariano", "sem-gluten"),
            base("Lasanha de Berinjela", "Camadas de berinjela grelhada, molho de tomate e muçarela gratinada, sem massa.", 3390, 40, "vegetariano", "sem-gluten"),
            base("Macarrão ao Pesto", "Espaguete com molho pesto de manjericão, castanhas e parmesão.", 2890, 20, "vegetariano"),
            base("Falafel Bowl", "Bolinhos de grão-de-bico, homus, tabule, picles de cebola roxa e pão sírio.", 2990, 25, "vegano"),
            base("Curry de Grão-de-Bico", "Grão-de-bico em curry cremoso de leite de coco com espinafre e arroz basmati.", 2790, 30, "vegano", "sem-gluten"),
            base("Salada Caesar com Frango", "Alface americana, frango grelhado, croutons, parmesão e molho caesar.", 2690, 15),
            base("Frango Grelhado com Legumes", "Peito de frango grelhado, legumes assados e purê de mandioca.", 2990, 30, "sem-gluten", "sem-lactose"),
            base("Escondidinho de Carne Seca", "Purê de mandioca gratinado com carne seca desfiada e queijo coalho.", 3590, 40, "sem-gluten"),
            base("Feijoada Completa", "Feijão preto com carnes suínas e bovinas, arroz, couve, farofa e laranja.", 3990, 50),
            base("Picanha na Chapa", "Picanha grelhada com arroz, farofa, vinagrete e batata frita.", 5490, 35, "sem-gluten", "sem-lactose"),
            base("Salmão ao Molho de Maracujá", "Filé de salmão grelhado com molho de maracujá, arroz de brócolis e legumes.", 5290, 30, "sem-gluten", "sem-lactose"),
            base("Bobó de Camarão", "Camarão em creme de mandioca com leite de coco e dendê, com arroz branco.", 4990, 40, "sem-gluten", "sem-lactose"),
            base("Tilápia com Pirão", "Tilápia assada com pirão de peixe, arroz e salada de tomate.", 3190, 35, "sem-gluten", "sem-lactose"),
            base("Frango à Parmegiana", "Filé de frango empanado com molho de tomate e queijo gratinado, arroz e batata frita.", 3490, 35),
            base("Yakisoba de Legumes", "Macarrão oriental salteado com legumes e molho shoyu.", 2790, 20, "vegano"),
            base("Yakisoba de Frango", "Macarrão oriental com frango, legumes e molho shoyu.", 3090, 20),
            base("Hambúrguer de Lentilha", "Hambúrguer de lentilha em pão integral, com alface, tomate, maionese de ervas e batata rústica.", 3190, 25, "vegano"),
            base("Hambúrguer Artesanal", "Blend bovino de 160 g, cheddar, cebola caramelizada e pão brioche, com batata frita.", 3690, 25),
            base("Pizza Marguerita", "Massa fina, molho de tomate, muçarela e manjericão.", 2890, 25, "vegetariano"),
            base("Tapioca de Queijo Coalho", "Tapioca recheada com queijo coalho e tomate seco.", 1890, 10, "vegetariano", "sem-gluten"),
            base("Omelete de Legumes", "Omelete de três ovos com legumes salteados e salada verde.", 2290, 15, "vegetariano", "sem-gluten", "sem-lactose"),
            base("Quinoa com Legumes Assados", "Quinoa com abóbora, brócolis e cenoura assados e molho de tahine.", 2890, 25, "vegano", "sem-gluten"),
            base("Poke de Atum", "Atum fresco, arroz de sushi, abacate, manga, pepino e molho de gergelim.", 4290, 15, "sem-lactose"),
            base("Poke Vegano de Tofu", "Tofu marinado, arroz, edamame, cenoura, manga e molho de gergelim.", 3290, 15, "vegano"),
            base("Bife Acebolado", "Bife grelhado com cebolas refogadas, arroz, feijão e batata frita.", 3290, 25, "sem-gluten", "sem-lactose"),
            base("Canja de Galinha", "Caldo de galinha com arroz, cenoura e cheiro-verde.", 2290, 30, "sem-gluten", "sem-lactose"),
            base("Sopa de Abóbora com Gengibre", "Creme de abóbora com gengibre e sementes torradas.", 2190, 25, "vegano", "sem-gluten"));

    public static List<DishSeed> generate() {
        List<DishSeed> dishes = new ArrayList<>();
        for (Base b : BASES) {
            List<String> tags = normalize(b.tags());
            int price = b.priceCents();
            int prep = b.prepMinutes();

            dishes.add(new DishSeed(b.name(), b.description(), price, prep, tags));
            dishes.add(new DishSeed(b.name() + " Fit",
                    b.description() + " Versão fit: porção leve, com menos gordura e mais legumes.",
                    price - 300, prep, tags));
            dishes.add(new DishSeed(b.name() + " Grande",
                    b.description() + " Porção 50% maior.",
                    price + 900, prep + 5, tags));
            dishes.add(new DishSeed(b.name() + " Família",
                    b.description() + " Serve de 3 a 4 pessoas.",
                    familyPrice(price), prep + 15, tags));
            dishes.add(new DishSeed(b.name() + " Express",
                    b.description() + " Versão express, de preparo rápido.",
                    price - 300, Math.max(10, prep / 2), tags));
        }
        return List.copyOf(dishes);
    }

    private static int familyPrice(int priceCents) {
        return ((priceCents * 12 / 5) / 100) * 100 + 90;
    }

    private static List<String> normalize(List<String> tags) {
        Set<String> normalized = new LinkedHashSet<>(tags);
        if (normalized.contains("vegano")) {
            normalized.add("vegetariano");
            normalized.add("sem-lactose");
        }
        return List.copyOf(normalized);
    }
}
