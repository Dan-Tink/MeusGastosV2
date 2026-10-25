# Meus Gastos

App Android (Kotlin + Jetpack Compose + Room) para registrar despesas como numa planilha.

## Como abrir e rodar

1. Instale o Android Studio (versão recente).
2. `File > Open` e escolha esta pasta (`MeusGastos`). Aguarde o Gradle sincronizar.
3. Para testar: conecte o celular com depuração USB ou crie um emulador e clique em Run.
4. Para gerar o APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## O que o app faz

- Painel do mês no topo: ganhos, gastos fixos, gastos variáveis e saldo, atualizados sozinhos.
- Comparação com o mesmo trecho do mês anterior e limite de gastos do mês (barra muda de cor em 80% e 100%).
- Lançamento rápido: tipo, categoria, descrição e valor; lembra o último tipo/categoria, sugere
  descrições já usadas e tem "Salvar e adicionar outro".
- Compras parceladas: informe o total e o número de parcelas; cada parcela cai em um mês.
- Categorias (Alimentação, Transporte, Lazer...) com gráfico de rosca no resumo.
- Gastos e ganhos recorrentes (aluguel, internet, salário) lançados sozinhos todo mês.
- Menu (três pontinhos): tipos, categorias, recorrentes, copiar fixos do mês anterior,
  limite e lembrete, exportar planilha (CSV), salvar e restaurar backup.
- Lembrete diário por notificação (só aparece se você ainda não lançou nada no dia).
- Busca por palavra-chave (em todos os meses) e filtros por tipo, categoria, valor e período.
- Dados salvos no aparelho (Room/SQLite), sem internet.
