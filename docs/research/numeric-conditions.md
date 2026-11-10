# Numeric conditions spike

## Question

Can Change Detection reliably read one number from a watched region and compare it to a threshold?

## Answer

Yes, if the person picks the number to watch. A narrow price region can be read across several writing conventions. A region with a sale price, an instalment or a range can contain multiple valid amounts, and the surrounding text does not give us a reliable universal way to choose.

## What was tried

Twelve excerpts from twelve public pages cover seven languages and seven currency styles. The fixtures record the page excerpts observed during the spike, so the tests do not depend on live prices.

| Sample | Text under test | Intended value |
| --- | --- | ---: |
| US starting price | `Buy from $899` | 899 |
| US price with cents | `From $899.00` | 899 |
| UK starting price | `From £1,199` | 1199 |
| German starting price | `Ab 1.449 €` | 1449 |
| French shelf price | `Prix 49,99€` | 49.99 |
| Brazilian starting price | `A partir de R$ 11.999` | 11999 |
| Brazilian price and instalment | `R$ 874,92/mês ou R$ 10.499` | 10499 |
| Swiss whole-franc price | `Ab CHF 1199.–` | 1199 |
| Indian price with rupees | `₹164900.00` | 164900 |
| Japanese price repeated | `¥8,999価格 ¥ 8999` | 8999 |
| Spanish trade-in maximum | `entre 150 € y 1.020 €` | 1020 |
| Italian trade-in maximum | `da € 150 a € 1.020` | 1020 |

Sources by sample order. [US starting prices](https://www.apple.com/shop/buy-iphone), [US model price](https://www.apple.com/shop/buy-iphone/iphone-17), [UK prices](https://www.apple.com/uk/shop/buy-iphone), [German prices](https://www.apple.com/de/shop/buy-iphone), [French bookcases](https://www.ikea.com/fr/fr/cat/bibliotheques-billy-58288/), [Brazilian prices](https://www.apple.com/br/shop/buy-iphone), [Brazilian payment options](https://www.apple.com/br/iphone/buy/), [Swiss prices](https://www.apple.com/ch-de/shop/buy-iphone), [Indian prices](https://www.apple.com/in/shop/buy-iphone), [Japanese bookcases](https://www.ikea.com/jp/ja/cat/billy-series-28102/), [Spanish trade-in](https://www.apple.com/es/shop/buy-iphone), [Italian trade-in](https://www.apple.com/it/shop/buy-iphone).

## What works

The intended amount is among the parsed candidates in all 12 fixtures. The samples cover prefixes and suffixes, decimal commas and periods, and common grouping conventions. The tests also cover space and apostrophe grouping, Indian grouping, unrelated counts and malformed tokens.

## What does not

`1.234` can be 1234 or a decimal with three places. The prototype chooses 1234 in that case. The Brazilian instalment excerpt contains two genuine prices. The Spanish and Italian samples each contain a genuine minimum and maximum. The Japanese excerpt repeats the same price, which produces two candidates. The sample set is dominated by retail prices and gives no estimate for quantities, stock quotes, negative values or page redesigns.

## Which number is “the” number

Taking the first amount with a currency gives the intended value in 9 of 12 samples. Taking the last gives 12 of 12 on this set, which places the full price and both selected range maxima last by construction. Requiring exactly one currency amount resolves 8 of 12. Picking a number explicitly resolves all 12 static excerpts, but the spike did not test whether the selection would survive a site redesign.

## What a feature would need

- Save the condition kind, threshold and selected amount per page.
- Evaluate it after text extraction and before deciding which alert to send.
- Name the amount, observed value and threshold in the alert.
- If the selected amount disappears or becomes ambiguous, send a normal change alert and explain why the condition could not be evaluated.

## Recommendation

Build a narrower feature for explicitly selected numbers in watched text. Validate the saved selection across multiple versions of the same pages before relying on it for alerts.
