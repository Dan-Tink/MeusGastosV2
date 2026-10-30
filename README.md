# Meus Gastos

App Android (Kotlin + Jetpack Compose + Room) para registrar despesas como numa planilha.

## Como abrir e rodar

1. Instale o Android Studio (versão recente).
2. `File > Open` e escolha esta pasta (`MeusGastos`). Aguarde o Gradle sincronizar.
3. Para testar: conecte o celular com depuração USB ou crie um emulador e clique em Run.
4. Para gerar o APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## O que o app faz

- Três abas: **Gastos** (só a lista, busca e filtros), **Resumo** (saldo do mês, ganhos, gastos fixos e
  variáveis, para onde foi o dinheiro) e **Ganhos**.
- Ganhos fixos (valem todo mês, sozinhos, até você parar; mudar o valor vale do mês atual em diante) e
  ganhos variáveis (valem só no mês em que foram lançados).
- Captura por notificação: lê as notificações de apps de banco e sugere cada compra, Pix ou pagamento em
  "Pendentes" para você confirmar (ou lança sozinho, se você ligar). Menu > Captura por notificação.
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

## Versão 1.2
- Novo visual verde (cabeçalho curvo, cartões arredondados, botão "+" central) com tema Automático/Claro/Escuro, salvo entre aberturas (menu ⋮ → Tema).
- Nova aba **Dívidas**: dívidas sem vencimento, pagamentos parciais com histórico, saldo devedor automático (centavos inteiros), edição segura e exclusão com confirmação. Pagamentos de dívida não viram despesas.
- Banco migrado de forma não destrutiva (6 → 7) e backup inclui dívidas e pagamentos.
