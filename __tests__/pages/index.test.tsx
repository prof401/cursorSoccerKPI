import { render, screen, waitFor } from "@testing-library/react";
import { fireEvent } from "@testing-library/react";
import axios from "axios";
import Home from "../../pages/index";

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe("Home page", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("shows create game form and title", () => {
    render(<Home />);
    expect(screen.getByText(/Soccer KPI Tracker/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Create Game/i })).toBeInTheDocument();
  });

  it("on Create Game click calls POST /games and shows gameId on success", async () => {
    const gameId = "test-game-123";
    mockedAxios.post.mockResolvedValueOnce({ data: { gameId } });
    render(<Home />);
    fireEvent.click(screen.getByRole("button", { name: /Create Game/i }));

    await waitFor(() => {
      expect(mockedAxios.post).toHaveBeenCalledWith(
        "https://api.example.com/games",
        expect.any(Object),
        expect.objectContaining({ headers: { "Content-Type": "application/json" } })
      );
    });
    await waitFor(() => {
      expect(screen.getByText(gameId)).toBeInTheDocument();
    });
  });
});
