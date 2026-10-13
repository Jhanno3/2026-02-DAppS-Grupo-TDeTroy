import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Avatar } from './Avatar'

describe('Avatar', () => {
  it('sin src, muestra las iniciales del nombre', () => {
    render(<Avatar src={null} nombre="Leonel Messi" />)

    expect(screen.getByText('LM')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('con un nombre de una sola palabra, usa sólo la primera letra', () => {
    render(<Avatar src={null} nombre="Prueba" />)

    expect(screen.getByText('P')).toBeInTheDocument()
  })

  it('con size="large", renderiza la imagen igual que el resto', () => {
    render(
      <Avatar
        src="https://example.com/foto.jpg"
        nombre="Leonel Messi"
        size="large"
      />,
    )

    expect(
      screen.getByRole('img', { name: 'Foto de Leonel Messi' }),
    ).toBeInTheDocument()
  })

  it('con src, renderiza la imagen con el alt correcto', () => {
    render(<Avatar src="https://example.com/foto.jpg" nombre="Leonel Messi" />)

    const img = screen.getByRole('img', { name: 'Foto de Leonel Messi' })
    expect(img).toHaveAttribute('src', 'https://example.com/foto.jpg')
  })

  it('si la imagen falla al cargar, cae a las iniciales', () => {
    render(<Avatar src="https://example.com/rota.jpg" nombre="Leonel Messi" />)

    fireEvent.error(screen.getByRole('img', { name: 'Foto de Leonel Messi' }))

    expect(screen.getByText('LM')).toBeInTheDocument()
    expect(
      screen.queryByRole('img', { name: 'Foto de Leonel Messi' }),
    ).not.toBeInTheDocument()
  })

  it('size="fill" con src válido, renderiza fondo difuminado + imagen completa', () => {
    render(
      <Avatar
        src="https://example.com/foto.jpg"
        nombre="Leonel Messi"
        size="fill"
      />,
    )

    expect(
      screen.getByRole('img', { name: 'Foto de Leonel Messi' }),
    ).toBeInTheDocument()
    // El backdrop decorativo está oculto para lectores de pantalla (aria-hidden), nunca aporta
    // al accessible name — sólo existe para rellenar el recuadro con la misma foto difuminada.
    expect(document.querySelectorAll('img')).toHaveLength(2)
  })

  it('size="fill" con error de carga, cae a las iniciales igual que el resto', () => {
    render(
      <Avatar
        src="https://example.com/rota.jpg"
        nombre="Leonel Messi"
        size="fill"
      />,
    )

    fireEvent.error(screen.getByRole('img', { name: 'Foto de Leonel Messi' }))

    expect(screen.getByText('LM')).toBeInTheDocument()
  })
})
