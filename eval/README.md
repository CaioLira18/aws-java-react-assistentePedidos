# Avaliação do assistente

Cada caso em `cases.json` descreve um pedido e o resultado esperado.
Avalie o comportamento verificável (quais tools foram chamadas, com quais filtros, o que foi parar no carrinho),
não a redação da resposta. Assim o teste é objetivo e repetível.

Rodar custa dinheiro (chama o modelo de verdade): execute manualmente ou em job agendado, nunca a cada commit.
Salve cada execução na tabela `eval_runs` e publique os números no README principal.

`cases.example.json` mostra o formato. Crie `cases.json` com 30 casos reais.
