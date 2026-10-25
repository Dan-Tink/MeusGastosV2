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
- Compras parceladas: escolha "À vista", 2x, 3x... 48x ou "Outro", e se o valor digitado é o da
  parcela (padrão) ou o total. Um resumo mostra parcela, total e mês da última. As parcelas ficam
  ligadas: ao editar ou excluir uma, o app pergunta se vale só para ela, para as próximas ou para todas.
- Categorias (Alimentação, Transporte, Lazer...) com gráfico de rosca no resumo.
- Gastos e ganhos recorrentes (aluguel, internet, salário) lançados sozinhos todo mês.
- Menu (três pontinhos): tipos, categorias, recorrentes, copiar fixos do mês anterior,
  limite e lembrete, exportar planilha (CSV), salvar e restaurar backup.
- Pago/pendente: gasto avulso nasce pago; fixo, recorrente e parcelado nascem pendentes. Toque no quadrado à esquerda para confirmar. O painel mostra "Pendente" e "Após pendentes".
- Aba Dívidas: anote o que você deve (ex.: empréstimo da irmã), abata pagamentos parciais ou marque "Paguei tudo". Não mexe no saldo do mês.
- Comprovante (foto ou PDF) anexado a cada gasto, guardado no aparelho (não vai no backup JSON).
- Lembrete diário por notificação (avisa de contas vencidas/a vencer e se você ainda não lançou nada no dia).
- Busca por palavra-chave (em todos os meses) e filtros por tipo, categoria, valor e período.
- Dados salvos no aparelho (Room/SQLite), sem internet.
