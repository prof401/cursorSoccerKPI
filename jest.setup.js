import "@testing-library/jest-dom";

// So that NEXT_PUBLIC_* is available when Next compiles pages in tests
process.env.NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://api.example.com";
