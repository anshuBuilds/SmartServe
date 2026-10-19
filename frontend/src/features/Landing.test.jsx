import {afterEach, describe, expect, it, vi} from 'vitest'
import {render, screen} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {Landing} from './Landing'
import {useAuth} from '../auth/AuthProvider'

vi.mock('../auth/AuthProvider', () => ({useAuth: vi.fn()}))

function renderLanding() {
  return render(
    <MemoryRouter>
      <Landing />
    </MemoryRouter>,
  )
}

describe('Landing', () => {
  afterEach(() => vi.clearAllMocks())

  it('sends visitors to staff login', () => {
    useAuth.mockReturnValue({user: null})
    renderLanding()

    expect(screen.getByRole('heading', {level: 1})).toHaveTextContent('From scan to served')
    const logins = screen.getAllByRole('link', {name: /staff login/i})
    expect(logins.length).toBeGreaterThan(0)
    logins.forEach(link => expect(link).toHaveAttribute('href', '/login'))
  })

  it("sends signed-in staff to their role's dashboard", () => {
    useAuth.mockReturnValue({user: {role: 'KITCHEN'}})
    renderLanding()

    const links = screen.getAllByRole('link', {name: /open dashboard/i})
    links.forEach(link => expect(link).toHaveAttribute('href', '/app/kitchen'))
    expect(screen.queryByRole('link', {name: /staff login/i})).not.toBeInTheDocument()
  })
})
