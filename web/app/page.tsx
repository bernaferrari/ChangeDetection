import { Footer } from "@/components/footer";
import { Navbar } from "@/components/navbar";
import { Features } from "@/components/features";
import { Hero } from "@/components/hero";
import { Showcase } from "@/components/showcase";
import { HowItWorks } from "@/components/how-it-works";
import { UseCases } from "@/components/use-cases";
import { CTA } from "@/components/cta";

export default function Home() {
  return (
    <>
      <Navbar />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Showcase />
        <UseCases />
        <CTA />
      </main>
      <Footer />
    </>
  );
}
