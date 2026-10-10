import type { Screen } from "./site"
import { screens } from "./site"

export type UseCase = {
  slug: string
  /** Search-style title for <title> and the card. */
  title: string
  h1: string
  description: string
  intro: string
  example: { label: string; before: string; after: string; context?: string }
  steps: { title: string; body: string }[]
  tips: string[]
  faqs: { q: string; a: string }[]
  screen: Screen
}

export const useCases: UseCase[] = [
  {
    slug: "price-drop-alert",
    title: "Price drop alerts for any online store",
    h1: "Get told the moment a price drops",
    description:
      "Track the price of any product page, on any store, and get a notification with the old and new price side by side. No extension, no account, no store-specific support needed.",
    intro:
      "Price trackers usually support a handful of big stores. Change Detection works on any product page, because it looks at the page itself: pick the price, and every check compares it with the last one. When it moves, you see the old and new price side by side.",
    example: { label: "Price", before: "$1,599.00", after: "$1,349.00", context: "Studio Display · store.example" },
    steps: [
      { title: "Open the product page", body: "Type the address or share it from your browser. The page opens in the built-in browser, so cookie banners, regions and logins work like they do in Safari or Chrome." },
      { title: "Pick the price", body: "Tap Parts, tap the price, and widen the selection with the slider if you want the stock line too. Only that part is compared, so a rotating banner never wakes you up." },
      { title: "Choose how often", body: "Hourly is plenty for most sales. For drops and flash deals, Turbo checks every 30 seconds while the app is open." },
    ],
    tips: [
      "Watching only the price (and maybe the stock line) keeps alerts quiet when recommendations or reviews change.",
      "Text mode is lighter on battery for prices; screenshots catch prices drawn by JavaScript or shown as images.",
      "Add the same product on two stores and compare which one moves first.",
    ],
    faqs: [
      { q: "Does it work with any store?", a: "Yes. It does not rely on store integrations: it opens the product page like a browser and compares what is on it, so it works with small shops as well as big marketplaces." },
      { q: "Will it notify me when only the recommendations change?", a: "Not if you pick the price. Picked parts are the only thing compared, and content that merely moved on the page is not reported as a change." },
    ],
    screen: screens.changes,
  },
  {
    slug: "restock-alert",
    title: "Restock alerts: know when it's back in stock",
    h1: "Back in stock? You'll know first",
    description:
      "Watch a sold-out product page and get notified when \"Sold out\" turns into \"Add to bag\". Works on any store, checks as often as every 30 seconds with Turbo.",
    intro:
      "Sold-out pages change in small ways when stock returns: a button label, a delivery estimate, a size that is no longer greyed out. Change Detection compares the part you pick and tells you exactly which words or pixels changed.",
    example: { label: "Availability", before: "Sold out · notify me", after: "In stock · free delivery", context: "Size M · shop.example" },
    steps: [
      { title: "Open the sold-out page", body: "Choose the exact size or colour first, so the page you watch is the one you want to buy." },
      { title: "Pick the buy button or the availability line", body: "That keeps the alert focused on stock, not on price tweaks or new photos." },
      { title: "Turn on Turbo for drops", body: "For limited releases, Turbo checks every 30 seconds while the app is open and shows a countdown to the next check." },
    ],
    tips: [
      "Screenshot mode is the safest choice for buttons that change colour or icon rather than text.",
      "If the store needs you to be signed in to see stock, sign in once in the built-in browser; checks reuse that session.",
    ],
    faqs: [
      { q: "How fast are restock alerts?", a: "Background checks follow the schedule you choose (hourly by default). With Turbo, pages are checked every 30 seconds while the app is open." },
      { q: "Can I watch pages that require a login?", a: "Yes. Sign in once in the app's browser; later checks reuse that session from your device." },
    ],
    screen: screens.turbo,
  },
  {
    slug: "job-posting-alert",
    title: "Job posting alerts for any careers page",
    h1: "New roles on a careers page, the day they're posted",
    description:
      "Watch a company's careers page or a filtered job board and get a clear list of new and removed roles, instead of re-reading the whole page.",
    intro:
      "Careers pages are long lists that reshuffle constantly. Change Detection reads them like a person would: new roles show up as new, closed roles as removed, and a list that was merely re-ordered is called out as \"only the order changed\" instead of a wall of noise.",
    example: { label: "Open roles", before: "4 open roles", after: "5 open roles · Product Designer (Remote)", context: "careers.example" },
    steps: [
      { title: "Open the filtered list", body: "Filter by team or location on the site first; the address with your filters is what gets watched." },
      { title: "Use Text mode", body: "Text comparison lists exactly which lines were added or removed, which is what you want for job titles." },
      { title: "Keep notifications on", body: "Each new posting arrives as a notification that opens straight to the change." },
    ],
    tips: [
      "Pick the list itself to ignore footers, cookie notices and \"featured\" banners.",
      "Re-ordered lists are recognised, so a shuffle does not look like ten new jobs.",
    ],
    faqs: [
      { q: "Does it work with job boards that load results with JavaScript?", a: "Yes. Pages are opened in a real browser engine, so content loaded by JavaScript is included." },
    ],
    screen: screens.text,
  },
  {
    slug: "exam-results-alert",
    title: "Exam results and admissions alerts",
    h1: "Stop refreshing the results page",
    description:
      "Watch an exam results, admissions or application status page and get notified the moment it updates, with the exact change highlighted.",
    intro:
      "Results day means refreshing the same page for hours. Let the app do it: it checks the page on a schedule (or every 30 seconds with Turbo) and tells you what changed, so you can put the phone down.",
    example: { label: "Status", before: "Results will be published soon", after: "Results are available: view your result", context: "results.example" },
    steps: [
      { title: "Open the status page", body: "If it needs a login, sign in in the app's browser; checks reuse the session." },
      { title: "Pick the status message", body: "Ignore the clock, the news ticker and anything else that changes every minute." },
      { title: "Use Turbo on the day", body: "Turbo keeps checking every 30 seconds while the app is open, with a countdown to the next check." },
    ],
    tips: ["If the site is overloaded and fails, the page shows a clear \"can't be checked\" state instead of a false alarm."],
    faqs: [
      { q: "Will it show my personal results to anyone else?", a: "No. Pages are opened and compared on your phone. There is no account and nothing is uploaded to a server." },
    ],
    screen: screens.detail,
  },
  {
    slug: "deadline-and-date-changes",
    title: "Track deadline and date changes on any page",
    h1: "Catch the date that quietly moved",
    description:
      "Applications, grants, events and visa pages change dates without announcing it. Watch the page and see exactly which date moved, old and new side by side.",
    intro:
      "A deadline moving from 31 October to 14 November is one word on a long page. Change Detection highlights exactly that word, struck through and replaced, so you notice it in a second.",
    example: { label: "Key dates", before: "Applications close 31 October", after: "Applications close 14 November", context: "Fellowship guide · opencall.example" },
    steps: [
      { title: "Open the page with the dates", body: "Program pages, event pages, official guidance: anything with a date that matters." },
      { title: "Use Text mode", body: "Edits are shown word by word, with the old text struck through and the new text highlighted." },
      { title: "Check daily", body: "Daily checks are enough for most deadlines and barely use battery or data." },
    ],
    tips: ["Number-only updates (counters, view counts) are grouped separately so a real date change stands out."],
    faqs: [
      { q: "What about pages where only a counter changes?", a: "Number-only changes are folded into their own section, so the meaningful edit is what you see first." },
    ],
    screen: screens.text,
  },
  {
    slug: "visual-website-monitoring",
    title: "Visual website monitoring with screenshot diffs",
    h1: "Screenshot diffs that ignore what only moved",
    description:
      "Compare screenshots of any web page and see only what really changed. Content pushed down by a new banner is recognised as moved, not changed, so you get a short list of real edits.",
    intro:
      "Pixel-by-pixel comparisons light up the whole page when a banner pushes everything down. Change Detection lines up the two screenshots like lines of text, forgives sub-pixel re-rendering, and still catches a single changed digit.",
    example: { label: "Headline", before: "Story 5: something happened in the city today", after: "Story 5: the mayor announced a brand new park", context: "news.example" },
    steps: [
      { title: "Open the page", body: "Screenshots are taken at phone width, signed in if you signed in, with cookie banners and ads hidden." },
      { title: "Ignore the noisy parts", body: "Tap Parts and mark carousels, clocks or ad slots as ignored; the rest is compared." },
      { title: "Step through changes", body: "Each change is a before/after crop; the Page view marks them on the full screenshot, one tap per change." },
    ],
    tips: [
      "Moved content is not reported as changed, so long news pages stay readable.",
      "Very small but dense edits, like one changed digit in a price, are still detected.",
    ],
    faqs: [
      { q: "How is this different from a pixel diff?", a: "A pixel diff marks everything below an inserted banner. This aligns the two captures first, so only content that really changed is shown." },
    ],
    screen: screens.page,
  },
]

export const findUseCase = (slug: string) => useCases.find((it) => it.slug === slug)
